package com.example.zim.api

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Users
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val client: HttpClient,
    private val userDao: UserDao,
    private val activeUserManager: ActiveUserManager,
    private val application: Application
) {
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    suspend fun fetchPosts() {
        try {
            // Use 10.0.2.2 instead of localhost when running in an emulator
            // This maps to the host machine's localhost
            val response: List<Post> = client.get("http://192.168.49.1:8080/posts").body()
            _posts.update { response }
        } catch (e: Exception) {
            // Log the error here
            println("Error fetching posts: ${e.message}")
            throw e
        }
    }

    suspend fun handshake(ip: String) {
        Toast.makeText(application,"Hand shake initiated",Toast.LENGTH_SHORT).show()
        try {
            val currentUser = userDao.getCurrentUser()
            val userData = UserData(
                fName = currentUser.users.fName,
                lName = currentUser.users.lName ?: "",
                publicKey = currentUser.users.UUID
            )
            val response: UserData = client.post("http://$ip:8080/user"){
                contentType(ContentType.Application.Json)
                // Set the body
                setBody(userData)
            }.body()
            // Check if a user with this public key exists in the database
            val existingUserId = userDao.getIdByUUID(response.publicKey)

            if (existingUserId == null) {
                // If user doesn't exist, insert the new user into the database
                val user = Users(
                    UUID = response.publicKey,
                    fName = response.fName,
                    lName = response.lName
                )
                userDao.insertUser(user)
                Log.d(
                    "ClientRepository",
                    "New user inserted: ${response.fName} ${response.lName}"
                )
            } else {
                // Update IP address if needed
                val existingUser = userDao.getUserById(existingUserId)
                if (existingUser.fName != response.fName || existingUser.lName != response.lName) {
                    val updatedUser = existingUser.copy(
                        fName = response.fName,
                        lName = response.lName
                    )
                    userDao.updateUser(updatedUser) // Using REPLACE conflict strategy
                    Log.d(
                        "ClientRepository",
                        "Updated Name: ${existingUser.fName} ${existingUser.lName}"
                    )

                } else {
                    Log.d(
                        "ClientRepository",
                        "User exists: ${existingUser.fName} ${existingUser.lName}"
                    )
                }
            }
            activeUserManager.addUser(response.publicKey,ip)
            Toast.makeText(application,"Hand shake successful",Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Log the error here
            Toast.makeText(application,"Hand shake Failed",Toast.LENGTH_SHORT).show()
            println("Error fetching posts: ${e.message}")
            throw e
        }
    }
}