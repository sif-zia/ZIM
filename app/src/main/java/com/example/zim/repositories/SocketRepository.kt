package com.example.zim.repositories

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject

class SocketRepository @Inject constructor(
    // Inject any dependencies if required
) {

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val _messages = MutableSharedFlow<String>() // For emitting received messages
    val messages: SharedFlow<String> get() = _messages

    private val _connectionStatus = MutableSharedFlow<Boolean>() // For emitting connection status
    val connectionStatus: SharedFlow<Boolean> get() = _connectionStatus

    suspend fun startServer(port: Int) = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(port)
            Log.d("Messages2", "Server started on port $port")
            clientSocket = serverSocket?.accept()
            Log.d("Messages2", "Client connected: ${clientSocket?.remoteSocketAddress}")
            setupStreams(clientSocket)
        } catch (e: Exception) {
            Log.d("Messages2", "Error starting server: ${e.message}")
        }
    }

    suspend fun connectToServer(ip: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            clientSocket = Socket(ip, port)
            Log.d("Messages2", "Connected to server at $ip:$port")
            setupStreams(clientSocket)
        } catch (e: Exception) {
            Log.d("Messages2", "Error connecting to server: ${e.message}")
        }
    }

    private suspend fun setupStreams(socket: Socket?) = withContext(Dispatchers.IO) {
        try {
            outputStream = socket?.getOutputStream()
            inputStream = socket?.getInputStream()
            Log.d("Messages2", "Streams set up successfully.")
            _connectionStatus.emit(true) // Emit connection status
            listenForMessages()
        } catch (e: Exception) {
            Log.d("Messages2", "Error setting up streams: ${e.message}")
        }
    }

    private suspend fun listenForMessages() = withContext(Dispatchers.IO) {
        try {
            val buffer = ByteArray(1024)
            while (true) {
                val bytesRead = inputStream?.read(buffer) ?: break
                val message = String(buffer, 0, bytesRead)
                _messages.emit(message) // Emit the received message
                Log.d("Messages2", "Message received: $message")
            }
        } catch (e: Exception) {
            Log.d("Messages2", "Error while listening for messages: ${e.message}")
            _connectionStatus.emit(false) // Emit connection status
        }
    }

    suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(message.toByteArray())
            outputStream?.flush()
            Log.d("Messages2", "Message sent: $message")
        } catch (e: Exception) {
            Log.d("Messages2", "Error sending message: ${e.message}")
        }
    }

    suspend fun closeConnection() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
            Log.d("Messages2", "Connection closed successfully.")
            _connectionStatus.emit(false) // Emit connection status
        } catch (e: Exception) {
            Log.d("Messages2", "Error closing connection: ${e.message}")
        }
    }
}
