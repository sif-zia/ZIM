package com.example.zim.api

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.Users
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val activeUserManager: ActiveUserManager,
    private val cryptoHelper: CryptoHelper,
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
                                        lName = userData.lName
                                    )
                                    userDao.insertUser(user)
                                    Log.d(
                                        TAG,
                                        "Server: New user inserted: ${userData.fName} ${userData.lName}"
                                    )
                                } else {
                                    // Update IP address if needed
                                    val existingUser = userDao.getUserById(existingUserId)
                                    if (existingUser.fName != userData.fName || existingUser.lName != userData.lName) {
                                        val updatedUser = existingUser.copy(
                                            fName = userData.fName,
                                            lName = userData.lName
                                        )
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

                                // Return user data as response
                                val responseData = UserData(
                                    publicKey = currentUser.users.UUID,
                                    fName = currentUser.users.fName,
                                    lName = currentUser.users.lName ?: "",
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(app, "Hand shake Successful", Toast.LENGTH_SHORT)
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
                                val messageData = call.receive<MessageData>()

                                val senderUUID = messageData.sender
                                val receiverUUID = messageData.receiver
                                val encryptedMessage = messageData.msg
                                val carrier = messageData.carrier

                                val carrierIp = call.request.origin.remoteHost
                                val myUuid = userDao.getCurrentUser().users.UUID

                                activeUserManager.addUser(carrier, carrierIp)

                                if (myUuid == receiverUUID) {
                                    val decryptedMessage = cryptoHelper.decryptMessage(
                                        encryptedMessage,
                                        senderUUID
                                    )
                                    insertReceivedMessage(senderUUID, decryptedMessage)
                                    call.respond(HttpStatusCode.OK, "Message received successfully")
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "Server: Error processing message data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid message data: ${e.message}"
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

    private suspend fun insertReceivedMessage(
        uuid: String,
        message: String,
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
                        msgIDFK = msgId.toInt()
                    )
                )
            }
        }
    }
}
