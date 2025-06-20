package com.example.zim.api

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.zim.batman.Acknowledgement
import com.example.zim.batman.BatmanProtocol
import com.example.zim.batman.MessagePayload
import com.example.zim.batman.OriginatorMessage
import com.example.zim.data.room.Dao.AlertDao
import com.example.zim.data.room.Dao.GroupDao
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.GroupMemberships
import com.example.zim.data.room.models.GroupMsgReceivers
import com.example.zim.data.room.models.Groups
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedAlerts
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.Users
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val alertDao: AlertDao,
    private val groupDao: GroupDao,
    private val activeUserManager: ActiveUserManager,
    private val batmanProtocol: BatmanProtocol,
    private val app: Application
) {
    private val TAG = "ApiRepository"

    // Create a coroutine scope for server operations
    private val serverScope = CoroutineScope(Dispatchers.IO)

    // Server state
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    // Store server instance to control it later
    private var server: ApplicationEngine? = null

    // Start the server
    fun startServer() {
        if (_isServerRunning.value) return

        // Use coroutine to avoid NetworkOnMainThreadException
        serverScope.launch {
            try {
                server = embeddedServer(Netty, port = 8080) {
                    // Configure JSON serialization
                    install(ContentNegotiation) {
                        json()
                    }

                    // Configure routing
                    routing {
                        post(ApiRoute.USER) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(app, "Hand shake Detected", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            try {
                                // Receive the user data from the request body
                                val userData = call.receive<UserData>()
                                val clientIpAddress = call.request.origin.remoteHost

                                // Add user to active map
                                activeUserManager.addUser(userData.publicKey, clientIpAddress)

                                // Check if a user with this public key exists in the database
                                val existingUserId = userDao.getIdByUUID(userData.publicKey)

                                if (existingUserId == null) {
                                    // If user doesn't exist, insert the new user into the database
                                    val user = Users(
                                        UUID = userData.publicKey,
                                        fName = userData.fName,
                                        lName = userData.lName,
                                        deviceName = userData.deviceName
                                    )
                                    userDao.insertUser(user)
                                    Log.d(
                                        TAG,
                                        "Server: New user inserted: ${userData.fName} ${userData.lName}"
                                    )
                                } else {
                                    // Update IP address if needed
                                    val existingUser = userDao.getUserById(existingUserId)
                                    if (existingUser.fName != userData.fName || existingUser.lName != userData.lName || existingUser.deviceName != userData.deviceName) {
                                        val updatedUser = existingUser.copy(
                                            fName = userData.fName,
                                            lName = userData.lName,
                                            deviceName = userData.deviceName,
                                        )
                                        userDao.activateUserById(existingUser.id) // Activate the user
                                        userDao.updateUser(updatedUser) // Using REPLACE conflict strategy
                                        Log.d(
                                            TAG,
                                            "Server: Updated Name: ${existingUser.fName} ${existingUser.lName}"
                                        )
                                    } else {
                                        Log.d(
                                            TAG,
                                            "Server: User exists: ${existingUser.fName} ${existingUser.lName}"
                                        )
                                    }
                                }

                                // Fetch the current user data to return as response
                                val currentUser = userDao.getCurrentUser()

                                if (currentUser == null) {
                                    Log.e(TAG, "Server: Current user not found")
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        "Current user not found"
                                    )
                                    return@post
                                }

                                // Return user data as response
                                val responseData = UserData(
                                    publicKey = currentUser.users.UUID,
                                    fName = currentUser.users.fName,
                                    lName = currentUser.users.lName ?: "",
                                    deviceName = currentUser.users.deviceName ?: ""
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        app,
                                        "Connected to ${userData.fName}",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                call.respond(responseData)
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing user data", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(app, "Hand shake Failed", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid user data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.MESSAGE) {
                            try {
                                val payload = call.receive<MessagePayload>()
                                val carrierIp = call.request.origin.remoteHost

                                activeUserManager.addUser(payload.senderAddress, carrierIp)

                                batmanProtocol.processMessage(payload)

                                call.respond(HttpStatusCode.OK, "Message received successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing message data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid message data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.IMAGE) {
                            val multipart = call.receiveMultipart()
                            var receiver = ""
                            var sender = ""
                            var fileName = ""
                            var messageId = ""
                            var fileExtension = "jpg" // Default extension
                            var fileBytes: ByteArray? = null

                            multipart.forEachPart { part ->
                                when (part) {
                                    is PartData.FormItem -> {
                                        when (part.name) {
                                            "receiver" -> receiver = part.value
                                            "sender" -> sender = part.value
                                            "fileExtension" -> fileExtension = part.value
                                            "fileName" -> fileName = part.value
                                            "messageId" -> messageId = part.value
                                        }
                                    }

                                    is PartData.FileItem -> {
                                        // If fileName already set from FormItem, use that, otherwise use the original
                                        if (fileName.isEmpty()) {
                                            fileName =
                                                part.originalFileName ?: "image.$fileExtension"
                                        }

                                        // If we don't have the extension yet, try to extract it from the filename
                                        if (fileExtension == "jpg" && fileName.contains(".")) {
                                            fileExtension = fileName.substringAfterLast('.', "jpg")
                                        }

                                        fileBytes = part.streamProvider().readBytes()
                                    }

                                    else -> {}
                                }
                                part.dispose()
                            }

                            val myUuid = userDao.getCurrentUser()?.users?.UUID
                            if (myUuid == null) {
                                Log.e(TAG, "Server: Current user not found")
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Current user not found"
                                )
                                return@post
                            }

                            if (receiver != myUuid) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        app,
                                        "Invalid image receiver",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                call.respond(HttpStatusCode.BadRequest, "Invalid image receiver")
                                return@post
                            }

                            fileBytes?.let {
                                // Save the image to disk with the proper extension
                                val uri = saveImageOnDisk(fileBytes!!, fileName, fileExtension)
                                if (uri != null) {
                                    insertReceivedMessage(
                                        sender,
                                        uri.toString(),
                                        messageId.toInt(),
                                        "Image"
                                    )
                                    call.respond(HttpStatusCode.OK, "Image received successfully")
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            app,
                                            "Unable to save image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        "Unable to save image"
                                    )
                                }
                            } ?: call.respond(HttpStatusCode.BadRequest, "Image not received")
                        }

                        post(ApiRoute.OGM) {
                            try {
                                val ogm = call.receive<OriginatorMessage>()

                                val ip = call.request.origin.remoteHost
                                val publicKey = ogm.senderAddress

                                activeUserManager.addUser(publicKey, ip)

                                batmanProtocol.processOGM(ogm)

                                call.respond(HttpStatusCode.OK, "OGM received successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing OGM data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid OGM data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.ACK) {
                            try {
                                val ack = call.receive<Acknowledgement>()

                                val ip = call.request.origin.remoteHost
                                val publicKey = ack.senderAddress

                                activeUserManager.addUser(publicKey, ip)

                                batmanProtocol.processAck(ack)

                                call.respond(HttpStatusCode.OK, "ACK received successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing ACK data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid ACK data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.ALERT) {
                            try {
                                val alert = call.receive<AlertData>()
                                val ip = call.request.origin.remoteHost
                                val publicKey = alert.alertSenderPuKey

                                activeUserManager.addUser(publicKey, ip)

                                Log.d(
                                    TAG,
                                    "Server: Alert received from ${alert.alertSenderFName} with ID : ${alert.alertId} about ${alert.alertType} with description ${alert.alertDescription} at ${alert.alertTime} and through public key ${alert.alertSenderPuKey}"
                                )
                                val user = userDao.getCurrentUser()
                                val myPuKey = user?.users?.UUID
                                if (myPuKey != alert.alertSenderPuKey) {
                                    //Process the alert data
                                    insertReceivedAlert(alert, ip)
                                    call.respond(HttpStatusCode.OK, "Alert received successfully")
                                }
                                else{
                                    Log.e(TAG, "Server:Alert Ignored due to self alert")
                                    call.respond(HttpStatusCode.OK, "Self Alert received ")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing ALERT data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid ALERT data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.GROUP_INVITE) {
                            try {
                                val groupInvite = call.receive<GroupInvite>()
                                val ip = call.request.origin.remoteHost
                                val publicKey = groupInvite.groupAdminPuKey

                                activeUserManager.addUser(publicKey, ip)

                                Log.d(
                                    TAG,
                                    "Server: Group invite received ${groupInvite.groupName} with description ${groupInvite.groupDescription} and through public key ${groupInvite.groupAdminPuKey}"
                                )

                                // Create the group in the database
                                val groupCreated = createGroup(groupInvite)

                                if(groupCreated)
                                    call.respond(HttpStatusCode.OK, "Group invite received successfully")
                                else
                                    call.respond(HttpStatusCode.BadRequest, "Group Not Created")
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing GROUP INVITE data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid GROUP INVITE data: ${e.message}"
                                )
                            }
                        }

                        post(ApiRoute.HELLO) {
                            try {
                                val helloData = call.receive<HelloData>()

                                val crrUser = userDao.getCurrentUser()?.users
                                if (crrUser == null) {
                                    Log.e(TAG, "Server: Current user not found")
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        "Current user not found"
                                    )
                                    return@post
                                }
                                val crrUserPublicKey = crrUser.UUID
                                val crrUserName = crrUser.fName + " " + crrUser.lName

                                if (helloData.publicKey != crrUserPublicKey) {
                                    call.respond(
                                        HttpStatusCode.OK,
                                        HelloData(crrUserName, crrUserPublicKey)
                                    )
                                }
                                call.respond(HttpStatusCode.BadRequest, "Self HELLO data")
                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing HELLO data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid HELLO data: ${e.message}"
                                )
                            }
                        }
                    }
                }

                server?.start(wait = false)
                _isServerRunning.value = true
                Log.d(TAG, "Server: Server started successfully on port 8080")
            } catch (e: Exception) {
                Log.e(TAG, "Server: Failed to start server", e)
            }
        }
    }

    // Stop the server
    fun stopServer() {
        serverScope.launch(Dispatchers.IO) {
            try {
                server?.stop(1000, 2000)
                server = null
                _isServerRunning.value = false
                activeUserManager.clearAllUsers()
                Log.d(TAG, "Server: Server stopped successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Server: Failed to stop server", e)
            }
        }
    }

    private suspend fun insertReceivedAlert(
        alert: AlertData,
        toSkipIp: String
    ) {
        // Use withContext instead of launch to ensure the function completes before returning
        withContext(serverScope.coroutineContext) {
            try {
                // Find or create user
                var userId = userDao.getIdByUUID(alert.alertSenderPuKey)
                if (userId == null) {
                    userId = userDao.insertUser(
                        Users(
                            UUID = alert.alertSenderPuKey,
                            fName = alert.alertSenderFName,
                            lName = alert.alertSenderLName,
                            isActive = false
                        )
                    ).toInt()
                }

                // Check if we already have this alert
                val oldAlert = alertDao.getAlert(alert.alertSenderPuKey, alert.alertId)

                if (oldAlert == null) {
                    // Insert new alert
                    val alertID = alertDao.insertAlert(
                        Alerts(
                            description = alert.alertDescription,
                            type = alert.alertType,
                            isSent = false,
                            sentTime = alert.alertTime
                        )
                    )

                    if (alertID > 0 && userId > 0) {
                        // Insert received alert with the ORIGINAL alert ID from sender
                        val receivedAlertId = alertDao.insertReceivedAlert(
                            ReceivedAlerts(
                                receivedTime = LocalDateTime.now(),
                                hops = alert.alertHops,
                                alertIdFk = alertID.toInt(),
                                initiatorIdFk = userId,
                                receivedAlertId = alert.alertId  // Use the original alert ID
                            )
                        ).toInt()

                        Log.d(TAG, "Server: Alert inserted successfully")

                        // Forward the alert immediately
                        batmanProtocol.forwardAlerts(
                            alertID.toInt(),
                            receivedAlertId,
                            userId,
                            toSkipIp
                        )
                    } else {
                        Log.e(TAG, "Server: Failed to insert alert")
                    }
                } else {
                    // Update existing alert only if the new hop count is lower
                    if (alert.alertHops < oldAlert.hops) {
                        alertDao.updateReceivedAlert(
                            oldAlert.id,
                            alert.alertHops,
                            LocalDateTime.now()  // Update received time to current time
                        )

                        // Forward the updated alert
                        batmanProtocol.forwardAlerts(
                            oldAlert.alertIdFk,
                            oldAlert.id,
                            userId,
                            toSkipIp
                        )
                    } else {
                        Log.d(TAG, "Server: Received alert with higher or equal hop count, not updating")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing received alert: ${e.message}", e)
            }
        }
    }


    private suspend fun insertReceivedMessage(
        uuid: String,
        message: String,
        messageId: Int,
        msgType: String = "Text"
    ) {
        val msgId = messageDao.insertMessage(
            Messages(
                msg = message,
                isSent = false,
                type = msgType
            )
        )
        val userId = userDao.getIdByUUID(uuid)

        if (userId != null) {
            if (msgId > 0 && userId > 0) {
                messageDao.insertReceivedMessage(
                    ReceivedMessages(
                        receivedTime = LocalDateTime.now(),
                        userIDFK = userId,
                        msgIDFK = msgId.toInt(),
                        receivedMessageId = messageId
                    )
                )
            }
        }
    }

    /**
     * Inserts a received message for a group chat
     *
     * @param senderUuid UUID of the user who sent the message
     * @param groupId ID of the group the message belongs to
     * @param message Content of the message
     * @param messageId Original ID of the message from the sender
     * @param msgType Type of message (default is "Text")
     */
    private suspend fun insertGroupReceivedMessage(
        senderUuid: String,
        groupId: Int,
        message: String,
        messageId: Int,
        msgType: String = "Text"
    ) {
        // Check if the message already exists
        val messageExists = messageDao.checkGroupMessageExist(groupId, senderUuid, messageId)
        if (messageExists) {
            return // Skip insertion if message already exists
        }

        // Insert the message with isDM = false to indicate it's a group message
        val msgId = messageDao.insertMessage(
            Messages(
                msg = message,
                isSent = false,
                type = msgType,
                isDM = false  // Important: Mark as not Direct Message
            )
        )

        // Get current user ID
        val currentUser = userDao.getCurrentUser()
        val currentUserId = currentUser?.users?.id ?: return

        // Get sender's user ID from UUID
        val senderId = userDao.getIdByUUID(senderUuid) ?: return

        if (msgId > 0) {
            // Insert into ReceivedMessages table
            messageDao.insertReceivedMessage(
                ReceivedMessages(
                    receivedTime = LocalDateTime.now(),
                    userIDFK = currentUserId,
                    msgIDFK = msgId.toInt(),
                    receivedMessageId = messageId
                )
            )

            // Insert into GroupMsgReceivers table to track message in the group
            messageDao.insertGroupMessageReceiver(
                GroupMsgReceivers(
                    msgIdFK = msgId.toInt(),
                    userIdFK = senderId,    // The sender's ID
                    groupIdFK = groupId
                )
            )
        }
    }

    private suspend fun saveImageOnDisk(
        imageData: ByteArray,
        fileName: String,
        fileExtension: String = "jpg"
    ): Uri? {
        return try {
            // Create a safe file extension (remove any dots and ensure lowercase)
            val safeExtension = fileExtension.replace(".", "").lowercase()

            // Generate a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val originalFileName = fileName.substringBeforeLast('.', "IMG")
            val finalFileName = "${originalFileName}_${timestamp}.$safeExtension"

            // Get the app's private pictures directory
            val imagesDir = File(
                app.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "received_images"
            )
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Create the file
            val imageFile = File(imagesDir, finalFileName)

            // Write the image data in chunks to avoid OutOfMemory errors with large images
            withContext(Dispatchers.IO) {
                FileOutputStream(imageFile).use { fos ->
                    val bufferSize = 1024 * 8 // 8KB chunks for writing
                    var offset = 0

                    while (offset < imageData.size) {
                        val chunkSize = minOf(bufferSize, imageData.size - offset)
                        fos.write(imageData, offset, chunkSize)
                        offset += chunkSize

                        // Optional: yield to prevent blocking the thread for too long
                        if (offset % (bufferSize * 10) == 0) {
                            yield() // Allow other coroutines to execute if needed
                        }
                    }

                    fos.flush()
                }
            }

            // Create a content URI for the saved image using FileProvider
            val imageUri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                imageFile
            )

            imageUri
        } catch (e: Exception) {
            Log.e("ImageReceiver", "Failed to save image", e)
            null // Return null if saving fails
        }
    }

    private suspend fun createGroup(groupInvite: GroupInvite): Boolean {
        val TAG = "GroupsViewModel"
        val alreadyExists = groupDao.checkIfGroupExists(groupInvite.groupSecretKey)
        if(alreadyExists) {
            Log.d(TAG, "Server: Group already exists")
            return true
        }

        var adminId: Int? = null
        groupInvite.groupMembers.forEach {
            var userId = userDao.getIdByUUID(it.puKey)
            if (userId == null) {
                userId = userDao.insertUser(
                    Users(
                        UUID = it.puKey,
                        fName = it.fName,
                        lName = it.lName,
                        isActive = false
                    )
                ).toInt()
            }
            if(it.puKey == groupInvite.groupAdminPuKey) {
                adminId = userId
            }
        }

        if(adminId == null) {
            Log.d(TAG, "Server: Admin not found in group members")
            return false
        }
        Log.d(TAG, "Server: Admin found in group members")

        val groupId = groupDao.insertGroup(Groups(
            admin = adminId!!,
            name = groupInvite.groupName,
            description = groupInvite.groupDescription,
            secretKey = groupInvite.groupSecretKey
        ))

        if(groupId < 0) {
            Log.d(TAG, "Server: Failed to create group")
            return false
        }
        Log.d(TAG, "Server: Group created successfully with ID: $groupId")

        groupInvite.groupMembers.forEach { user ->
            val userId = userDao.getIdByUUID(user.puKey)
            if (userId != null) {
                groupDao.insertGroupMember(
                    GroupMemberships(
                        groupId = groupId.toInt(),
                        userId = userId,
                        hasReceivedInvitation = false
                    )
                )
            }
        }
        Log.d(TAG, "Server: Group members added successfully")
        return true
    }
}
