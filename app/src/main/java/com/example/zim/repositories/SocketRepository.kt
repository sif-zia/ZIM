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

class SocketRepository @Inject constructor() {

    // Data class to hold socket streams and references
    private data class SocketConnection(
        val socket: Socket,
        val inputStream: InputStream,
        val outputStream: OutputStream
    )

    private val serverSockets = mutableMapOf<String, ServerSocket>() // Map of UUID to ServerSocket
    private val connections = mutableMapOf<String, SocketConnection>() // Map of UUID to client connections

    private val _messages = MutableSharedFlow<Pair<String, String>>() // Pair(UUID, Message)
    val messages: SharedFlow<Pair<String, String>> get() = _messages

    private val _connectionStatus = MutableSharedFlow<Pair<String, Boolean>>() // Pair(UUID, Status)
    val connectionStatus: SharedFlow<Pair<String, Boolean>> get() = _connectionStatus

    // Start a server socket
    suspend fun startServer(uuid: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            val serverSocket = ServerSocket(port)
            serverSockets[uuid] = serverSocket
            Log.d("Messages2", "Server started on port $port with UUID: $uuid")
            while (!serverSocket.isClosed) {
                val socket = serverSocket.accept()
                val connectionUuid = uuid // Use the same UUID for this server connection
                setupStreams(connectionUuid, socket)
                Log.d("Messages2", "Client connected to server with UUID: $connectionUuid")
            }
        } catch (e: Exception) {
            Log.d("Messages2", "Error starting server $uuid: ${e.message}")
        }
    }

    // Connect to another server as a client
    suspend fun connectToServer(uuid: String, ip: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            val socket = Socket(ip, port)
            Log.d("Messages2", "Connected to server at $ip:$port with client UUID: $uuid")
            setupStreams(uuid, socket)
        } catch (e: Exception) {
            Log.d("Messages2", "Error connecting to server with client UUID $uuid: ${e.message}")
        }
    }

    // Set up streams for a socket and start listening for messages
    private suspend fun setupStreams(uuid: String, socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            connections[uuid] = SocketConnection(socket, inputStream, outputStream)
            _connectionStatus.emit(uuid to true) // Emit connection status
            listenForMessages(uuid, inputStream)
        } catch (e: Exception) {
            Log.d("Messages2", "Error setting up streams for $uuid: ${e.message}")
        }
    }

    // Listen for messages from a specific socket
    private suspend fun listenForMessages(uuid: String, inputStream: InputStream) = withContext(Dispatchers.IO) {
        try {
            val buffer = ByteArray(1024)
            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead > 0) {
                    val message = String(buffer, 0, bytesRead)
                    _messages.emit(uuid to message) // Emit the received message with UUID
                    Log.d("Messages2", "Message received from $uuid: $message")
                }
            }
        } catch (e: Exception) {
            Log.d("Messages2", "Error while listening for messages from $uuid: ${e.message}")
            _connectionStatus.emit(uuid to false) // Emit connection status
            closeConnection(uuid)
        }
    }

    // Send a message to a specific socket
    suspend fun sendMessage(uuid: String, message: String) = withContext(Dispatchers.IO) {
        try {
            connections[uuid]?.outputStream?.apply {
                write(message.toByteArray())
                flush()
                Log.d("Messages2", "Message sent to $uuid: $message")
            }
        } catch (e: Exception) {
            Log.d("Messages2", "Error sending message to $uuid: ${e.message}")
        }
    }

    // Close a specific connection
    suspend fun closeConnection(uuid: String) = withContext(Dispatchers.IO) {
        try {
            connections[uuid]?.apply {
                inputStream.close()
                outputStream.close()
                socket.close()
                Log.d("Messages2", "Connection with UUID $uuid closed successfully.")
            }
            connections.remove(uuid)
            _connectionStatus.emit(uuid to false) // Emit connection status
        } catch (e: Exception) {
            Log.d("Messages2", "Error closing connection with UUID $uuid: ${e.message}")
        }
    }

    // Close a specific server socket
    suspend fun closeServer(uuid: String) = withContext(Dispatchers.IO) {
        try {
            serverSockets[uuid]?.close()
            serverSockets.remove(uuid)
            Log.d("Messages2", "Server socket with UUID $uuid closed successfully.")
        } catch (e: Exception) {
            Log.d("Messages2", "Error closing server socket with UUID $uuid: ${e.message}")
        }
    }

    // Close all connections and servers
    suspend fun closeAll() = withContext(Dispatchers.IO) {
        try {
            connections.forEach { (uuid, connection) ->
                connection.inputStream.close()
                connection.outputStream.close()
                connection.socket.close()
                _connectionStatus.emit(uuid to false) // Emit connection status
            }
            connections.clear()
            serverSockets.forEach { (uuid, serverSocket) ->
                serverSocket.close()
                Log.d("Messages2", "Server socket with UUID $uuid closed successfully.")
            }
            serverSockets.clear()
            Log.d("Messages2", "All connections and server sockets closed successfully.")
        } catch (e: Exception) {
            Log.d("Messages2", "Error closing all sockets and servers: ${e.message}")
        }
    }
}
