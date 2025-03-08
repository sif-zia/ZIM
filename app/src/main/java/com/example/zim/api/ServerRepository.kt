package com.example.zim.api

import android.util.Log
import android.widget.Toast
import com.example.zim.data.room.Dao.UserDao
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val userDao: UserDao,
    private val activeUserManager: ActiveUserManager,
) {
    // Create a coroutine scope for server operations
    private val serverScope = CoroutineScope(Dispatchers.IO)

    // Server state
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    // Store server instance to control it later
    private var server: ApplicationEngine? = null

    // Generate dummy Post data
    private fun generateDummyPosts(): List<Post> {
        return (1..100).map { id ->
            Post(
                userId = (id - 1) / 10 + 1,
                id = id,
                title = "Post $id title",
                body = "This is the body of post $id with some sample content."
            )
        }
    }

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
                        post("/user") {
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
                                        "ServerRepository",
                                        "New user inserted: ${userData.fName} ${userData.lName}"
                                    )
                                }
                                else {
                                    // Update IP address if needed
                                    val existingUser = userDao.getUserById(existingUserId)
                                    if (existingUser.fName != userData.fName || existingUser.lName != userData.lName) {
                                        val updatedUser = existingUser.copy(
                                            fName = userData.fName,
                                            lName = userData.lName
                                        )
                                        userDao.updateUser(updatedUser) // Using REPLACE conflict strategy
                                        Log.d(
                                            "ServerRepository",
                                            "Updated Name: ${existingUser.fName} ${existingUser.lName}"
                                        )
                                    } else {
                                        Log.d(
                                            "ServerRepository",
                                            "User exists: ${existingUser.fName} ${existingUser.lName}"
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
                                call.respond(responseData)
                            } catch (e: Exception) {
                                Log.e("ServerRepository", "Error processing user data", e)
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid user data: ${e.message}"
                                )
                            }
                        }

                        get("/posts") {
                            call.respond(generateDummyPosts())
                        }
                    }
                }

                server?.start(wait = false)
                _isServerRunning.value = true
                Log.d("ServerProvider", "Server started successfully on port 8080")
            } catch (e: Exception) {
                Log.e("ServerProvider", "Failed to start server", e)
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
                Log.d("ServerProvider", "Server stopped successfully")
            } catch (e: Exception) {
                Log.e("ServerProvider", "Failed to stop server", e)
            }
        }
    }
}
