package com.example.zim.api

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.example.zim.batman.Acknowledgement
import com.example.zim.batman.MessagePayload
import com.example.zim.batman.OriginatorMessage
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.UserWithCurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.wifiP2P.WifiDirectManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val application: Application,
    private val client: HttpClient,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val activeUserManager: ActiveUserManager,
    private val wifiDirectManager: WifiDirectManager
) {
//    init {
//        observeConnection()
//    }

    val ips: MutableList<String> = mutableListOf()
    val handshakeMutex = kotlinx.coroutines.sync.Mutex()

    private fun getURL(ip: String, route: String): String {
        return "${ApiRoute.BASE_URL}$ip${ApiRoute.PORT}$route"
    }

    suspend fun handshake(isGroupOwner: Boolean, connectedDevices: Int, groupOwnerIp: String) {
        handshakeMutex.withLock {
            try {
                val subnet = groupOwnerIp.substringBeforeLast('.')
                val toConnectIps = generatePotentialIpAddresses(subnet, isGroupOwner, connectedDevices)

                val currentUser = userDao.getCurrentUser()
                val userData = createUserData(currentUser)

                for (ip in toConnectIps) {
                    if (!ips.contains(ip)) {
                        tryConnectToDevice(ip, userData)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client: Error in Handshake process: ${e.message}", e)
                showToast("Handshake Failed")
            }
        }
    }

    private fun generatePotentialIpAddresses(subnet: String, isGroupOwner: Boolean, connectedDevices: Int): List<String> {
        return if (isGroupOwner) {
            // For group owner, client IPs typically start from .2
            (2 until connectedDevices + 2).map { "$subnet.$it" }
        } else {
            // For clients, try both .1 (group owner) and other potential clients
            (1 until connectedDevices + 2).map { "$subnet.$it" }
        }
    }

    private fun createUserData(currentUser: UserWithCurrentUser): UserData {
        return UserData(
            fName = currentUser.users.fName,
            lName = currentUser.users.lName ?: "",
            publicKey = currentUser.users.UUID,
            deviceName = currentUser.users.deviceName ?: ""
        )
    }

    private suspend fun tryConnectToDevice(ip: String, userData: UserData) {
        try {

            Log.e(TAG, "Handshake initiated with ip: $ip")
            val response = sendHandshakeRequest(ip, userData)

            // Skip self-connections
            if (response.publicKey == userData.publicKey) {
                return
            }

            processUserResponse(response, ip)

        } catch (e: Exception) {
            userDisconnected(ip)
            Log.d(TAG, "Client: Failed to connect to $ip: ${e.message}")
            // Don't show toast for individual connection failures to avoid spamming
        }
    }

    private suspend fun sendHandshakeRequest(ip: String, userData: UserData): UserData {
        return client.post(getURL(ip, ApiRoute.USER)) {
            contentType(ContentType.Application.Json)
            setBody(userData)
        }.body()
    }

    private suspend fun processUserResponse(response: UserData, ip: String) {
        try {
            val userId = processUserData(response)
            activeUserManager.addUser(response.publicKey, ip)
            wifiDirectManager.addConnectedDevice(response)
            ips.add(ip)
            showToast("Connected to ${response.fName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing user data: ${e.message}", e)
        }
    }

    private suspend fun processUserData(userData: UserData): Int {
        val existingUserId = userDao.getIdByUUID(userData.publicKey)

        return if (existingUserId == null) {
            insertNewUser(userData)
        } else {
            updateExistingUserIfNeeded(existingUserId, userData)
            existingUserId
        }
    }

    private suspend fun insertNewUser(userData: UserData): Int {
        val user = Users(
            UUID = userData.publicKey,
            fName = userData.fName,
            lName = userData.lName,
            deviceName = userData.deviceName
        )
        val id = userDao.insertUser(user).toInt()
        Log.d(TAG, "Client: New user inserted: ${userData.fName} ${userData.lName}")
        return id
    }

    private suspend fun updateExistingUserIfNeeded(userId: Int, userData: UserData): Boolean {
        val existingUser = userDao.getUserById(userId)

        if (existingUser.fName != userData.fName ||
            existingUser.lName != userData.lName ||
            existingUser.deviceName != userData.deviceName) {

            val updatedUser = existingUser.copy(
                fName = userData.fName,
                lName = userData.lName,
                deviceName = userData.deviceName
            )
            userDao.updateUser(updatedUser)
            Log.d(TAG, "Client: Updated user: ${userData.fName} ${userData.lName}")
            return true
        } else {
            Log.d(TAG, "Client: User exists: ${existingUser.fName} ${existingUser.lName}")
            return false
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun sendMessage(message: MessagePayload, neighborIp: String): Boolean {
        try {
            val response = client.post(getURL(neighborIp, ApiRoute.MESSAGE)){
                contentType(ContentType.Application.Json)
                setBody(message)
            }

            if(response.status == HttpStatusCode.OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Message sent", Toast.LENGTH_SHORT).show()
                }
                return true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Message failed", Toast.LENGTH_SHORT).show()
                }
                userDisconnected(neighborIp)
                return false
            }
        } catch (e: Exception) {
            // Log the error here
            userDisconnected(neighborIp)
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Message failed", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG,"Client: Error sending message: ${e.message}")
            return false
        }
    }

    suspend fun sendImage(imageUri: Uri, receiverId: Int) {
        val receiver = userDao.getUserById(receiverId).UUID
        try {
            if(!activeUserManager.hasUser(receiver)) {
                // Log the error here
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Message failed", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val byteArray = application.contentResolver.openInputStream(imageUri)?.use {
                it.readBytes()
            } ?: throw IOException("Could not read image")

            // Get the file's mime type
            val mimeType = application.contentResolver.getType(imageUri) ?: "image/jpeg"

            // Extract the file extension from the mime type or URI
            val fileExtension = when {
                mimeType != "application/octet-stream" -> mimeType.split("/").lastOrNull() ?: "jpeg"
                else -> {
                    // Fallback: try to get extension from URI path
                    val path = getFilePathFromUri(imageUri)
                    path?.substringAfterLast('.')?.takeIf { it.isNotEmpty() } ?: "jpeg"
                }
            }

            // Get original filename if possible
            val filename = getFileNameFromUri(imageUri) ?: "image.$fileExtension"
            val myUuid = userDao.getCurrentUser().users.UUID

            insertSentMessage(receiverId, imageUri.toString(),"Image")

            val response = client.submitFormWithBinaryData(
                url = getURL(activeUserManager.getIpAddressForUser(receiver)!!, ApiRoute.IMAGE),
                formData = formData {
                    append("receiver", receiver)
                    append("sender", myUuid)
                    append("fileExtension", fileExtension)
                    append("fileName", filename)
                    append("image", byteArray, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=$filename")
                    })
                }
            )

            if(response.status == HttpStatusCode.OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Image sent", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Image failed", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            // Log the error here
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Image failed", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG,"Client: Error sending message: ${e.message}")
        }
    }

    suspend fun sendOGM(ogm: OriginatorMessage, ip: String): Boolean {
        try {
            val response = client.post(getURL(ip, ApiRoute.OGM)) {
                contentType(ContentType.Application.Json)
                setBody(ogm)
            }
            return response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            // Log the error here
            Log.d(TAG,"Client: Error sending OGM: ${e.message}")
            return false
        }
    }

    suspend fun sendAck(ack: Acknowledgement, ip: String): Boolean {
        try {
            val response = client.post(getURL(ip, ApiRoute.ACK)) {
                contentType(ContentType.Application.Json)
                setBody(ack)
            }
            return response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            // Log the error here
            userDisconnected(ip)
            Log.d(TAG,"Client: Error sending ACK: ${e.message}")
            return false
        }
    }


    private suspend fun insertSentMessage(userId: Int, message: String, msgType: String = "Text") {
        val uuid = userDao.getUserById(userId).UUID
        val status = if (activeUserManager.hasUser(uuid)) "Sent" else "Sending"

        val msgId = messageDao.insertMessage(Messages(msg = message, isSent = true, type = msgType))

        if (msgId > 0 && userId > 0) {
            messageDao.insertSentMessage(
                SentMessages(
                    sentTime = LocalDateTime.now(),
                    userIDFK = userId,
                    status = status,
                    msgIDFK = msgId.toInt()
                )
            )
        }
    }

//    private suspend fun sendPendingMessages(uuid: String) {
//        // Use a mutex to ensure thread safety when accessing the pending messages
//        val mutex = kotlinx.coroutines.sync.Mutex()
//
//        mutex.withLock {
//            // Get pending messages within the lock to ensure consistency
//            val messages = messageDao.getPendingMessages(uuid)
//
//            try {
//                // Process each message within the lock
//
//                for (message in messages) {
//                    delay(50)
////                    sendMessage(userDao.getIdByUUID(uuid), message)
//                    // Add a small delay to prevent overwhelming the socket
//
//                    sendMessage(message, userDao.getIdByUUID(uuid)!!, false)
//
//
//                    Log.d(TAG, "Sending message to ${uuid}")
//                }
//
//                // Mark messages as sent only if the loop completes successfully
//                messageDao.markPendingMessagesAsSent(uuid)
//            } catch (e: Exception) {
//                Log.e(TAG, "Client: Error sending pending messages: ${e.message}", e)
//                // If there's an error, we don't mark messages as sent
//            }
//        }
//    }

    companion object {
        private const val TAG = "ApiRepository"
    }

//    private fun observeConnection() {
//        CoroutineScope(Dispatchers.IO).launch {
//            activeUserManager.activeUsers.collect { users ->
//                users.forEach { (publicKey, _) ->
//                    sendPendingMessages(publicKey)
//                }
//            }
//        }
//    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null

        // Try query for display name
        application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        // If we couldn't get it from the content provider, try the path
        if (fileName == null) {
            fileName = uri.path?.substringAfterLast('/')
        }

        return fileName
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        // Handle different URI schemes
        when {
            // File URI
            uri.scheme.equals("file", ignoreCase = true) -> return uri.path

            // Content URI
            uri.scheme.equals("content", ignoreCase = true) -> {
                var cursor: Cursor? = null
                try {
                    cursor = application.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        return cursor.getString(columnIndex)
                    }
                } catch (e: Exception) {
                    Log.e("URI_HELPER", "Failed to get path from URI", e)
                } finally {
                    cursor?.close()
                }
            }
        }
        return null
    }

    private fun userDisconnected(ip: String) {
        activeUserManager.getUserByIp(ip)?.let { uuid ->
            activeUserManager.removeUser(uuid)
            wifiDirectManager.removeConnectedDevice(uuid)
        }
        ips.remove(ip)
    }
}