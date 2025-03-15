package com.example.zim.api

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.example.zim.batman.OriginatorMessage
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.wifiP2P.WifiDirectManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val client: HttpClient,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val activeUserManager: ActiveUserManager,
    private val application: Application,
    private val cryptoHelper: CryptoHelper,
    private val wifiDirectManager: WifiDirectManager
) {
    init {
        observeConnection()
    }

    val ips: MutableList<String> = mutableListOf()

    private fun getURL(ip: String, route: String): String {
        return "${ApiRoute.BASE_URL}$ip${ApiRoute.PORT}$route"
    }

    suspend fun handshake(ip: String): Boolean {
        ips.forEach {
            if(it == ip) {
                return false
            }
        }
        ips.add(ip)

        withContext(Dispatchers.Main) {
            Toast.makeText(application, "Hand shake initiated", Toast.LENGTH_SHORT).show()
        }
        try {
            val currentUser = userDao.getCurrentUser()
            val userData = UserData(
                fName = currentUser.users.fName,
                lName = currentUser.users.lName ?: "",
                publicKey = currentUser.users.UUID,
                deviceName = currentUser.users.deviceName ?: ""
            )
            val response: UserData = client.post(getURL(ip, ApiRoute.USER)) {
                contentType(ContentType.Application.Json)
                // Set the body
                setBody(userData)
            }.body()

            if(response.publicKey == currentUser.users.UUID) {
                return false
            }

            // Check if a user with this public key exists in the database
            val existingUserId = userDao.getIdByUUID(response.publicKey)

            if (existingUserId == null) {
                // If user doesn't exist, insert the new user into the database
                val user = Users(
                    UUID = response.publicKey,
                    fName = response.fName,
                    lName = response.lName,
                    deviceName = response.deviceName
                )
                userDao.insertUser(user)
                Log.d(TAG,
                    "Client: New user inserted: ${response.fName} ${response.lName}"
                )
            } else {
                // Update IP address if needed
                val existingUser = userDao.getUserById(existingUserId)
                if (existingUser.fName != response.fName || existingUser.lName != response.lName || existingUser.deviceName != response.deviceName) {
                    val updatedUser = existingUser.copy(
                        fName = response.fName,
                        lName = response.lName,
                        deviceName = response.deviceName
                    )
                    userDao.updateUser(updatedUser) // Using REPLACE conflict strategy
                    Log.d(
                        TAG,
                        "Client: Updated Name: ${existingUser.fName} ${existingUser.lName}"
                    )

                } else {
                    Log.d(
                        TAG,
                        "Client: User exists: ${existingUser.fName} ${existingUser.lName}"
                    )
                }
            }
            activeUserManager.addUser(response.publicKey, ip)
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Hand shake successful", Toast.LENGTH_SHORT).show()
            }
            wifiDirectManager.addConnectedDevice(response)
            return true
        } catch (e: Exception) {
            // Log the error here
            ips.remove(ip)
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Hand shake Failed", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG,"Client: Error in Handshake: ${e.message}")
            return false
        }
    }

    suspend fun sendMessage(message: String, userId: Int, toInsert: Boolean = true) {
        try {
            val receiver = userDao.getUserById(userId).UUID

            if(message.isEmpty() || receiver.isEmpty()) {
                return
            }

            if(toInsert)
                insertSentMessage(userId, message)

            if(!activeUserManager.hasUser(receiver)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "User not connected", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val ip = activeUserManager.getIpAddressForUser(receiver)
            val encryptedMessage = cryptoHelper.encryptMessage(message, receiver)
            val currentUser = userDao.getCurrentUser()

            val messageData = MessageData(
                sender = currentUser.users.UUID,
                receiver = receiver,
                carrier = currentUser.users.UUID,
                msg = encryptedMessage
            )

            val response = client.post(getURL(ip!!, ApiRoute.MESSAGE)){
                contentType(ContentType.Application.Json)
                // Set the body
                setBody(messageData)
            }
            if(response.status == HttpStatusCode.OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Message sent", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Message failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Log the error here
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Message failed", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG,"Client: Error sending message: ${e.message}")
            throw e
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
                // Set the body
                setBody(ogm)
            }

            if(response.status == HttpStatusCode.OK) {
                return true
            }
            return false
        } catch (e: Exception) {
            // Log the error here
            Log.d(TAG,"Client: Error sending OGM: ${e.message}")
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

    private suspend fun sendPendingMessages(uuid: String) {
        // Use a mutex to ensure thread safety when accessing the pending messages
        val mutex = kotlinx.coroutines.sync.Mutex()

        mutex.withLock {
            // Get pending messages within the lock to ensure consistency
            val messages = messageDao.getPendingMessages(uuid)

            try {
                // Process each message within the lock

                for (message in messages) {
                    delay(50)
//                    sendMessage(userDao.getIdByUUID(uuid), message)
                    // Add a small delay to prevent overwhelming the socket

                    sendMessage(message, userDao.getIdByUUID(uuid)!!, false)


                    Log.d(TAG, "Sending message to ${uuid}")
                }

                // Mark messages as sent only if the loop completes successfully
                messageDao.markPendingMessagesAsSent(uuid)
            } catch (e: Exception) {
                Log.e(TAG, "Client: Error sending pending messages: ${e.message}", e)
                // If there's an error, we don't mark messages as sent
            }
        }
    }

    companion object {
        private const val TAG = "ApiRepository"
    }

    private fun observeConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            activeUserManager.activeUsers.collect { users ->
                users.forEach { (publicKey, _) ->
                    sendPendingMessages(publicKey)
                }
            }
        }
    }

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
}