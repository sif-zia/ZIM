package com.example.zim.repositories

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class SocketService(private val scope: CoroutineScope) {

    private var socket: Socket? = null // For client mode
    private var serverSocket: ServerSocket? = null // For server mode
    private val connections = mutableMapOf<String, Connection>() // Map of UUID to Connection

    // SharedFlow to emit connection status (UUID to Boolean)
    private val _connectionStatus = MutableSharedFlow<Pair<String, Boolean>>(replay = 10)
    val connectionStatus: SharedFlow<Pair<String, Boolean>> get() = _connectionStatus

    // Data class to hold reader and writer for each connection
    private data class Connection(
        val socket: Socket,
        val reader: BufferedReader,
        val writer: PrintWriter
    )

    sealed class Mode {
        object Client : Mode()
        object Server : Mode()
    }

    /**
     * Starts the socket service in either client or server mode.
     *
     * @param mode The mode (Client or Server).
     * @param uuid The UUID to identify the connection.
     * @param host The host to connect to (for client mode).
     * @param port The port to connect to or listen on.
     * @param onMessageReceived Callback to handle received messages (UUID, message).
     */
    fun start(mode: Mode, uuid: String, host: String? = null, port: Int, onMessageReceived: (String, String) -> Unit) {
        when (mode) {
            is Mode.Client -> connectAsClient(uuid, host ?: "192.168.49.1", port, onMessageReceived)
            is Mode.Server -> startAsServer(uuid, port, onMessageReceived)
        }
    }

    private fun connectAsClient(uuid: String, host: String, port: Int, onMessageReceived: (String, String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                socket = Socket(host, port)
                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                val writer = PrintWriter(socket!!.getOutputStream(), true)

                // Store the connection
                connections[uuid] = Connection(socket!!, reader, writer)

                // Emit connection status: connected
                Log.d("SocketService", "Connected to $host:$port with UUID: $uuid")
                _connectionStatus.emit(uuid to true)

                while (true) {
                    val message = reader.readLine() ?: break
                    onMessageReceived(uuid, message) // Pass the UUID and message
                    Log.d("SocketService", "Received message from $uuid: $message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("SocketService", "Error occurred while connecting to $host:$port: ${e.message}")
            } finally {
                // Emit connection status: disconnected
                _connectionStatus.emit(uuid to false)
                disconnect(uuid)
            }
        }
    }

    private fun startAsServer(uuid: String, port: Int, onMessageReceived: (String, String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)

                while (true) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                        val writer = PrintWriter(clientSocket.getOutputStream(), true)

                        // Store the connection
                        connections[uuid] = Connection(clientSocket, reader, writer)

                        // Emit connection status: client connected
                        Log.d("SocketService", "Client with UUID: $uuid connected")
                        _connectionStatus.emit(uuid to true)

                        handleClient(uuid, onMessageReceived)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("SocketService", "Error occurred while starting server: ${e.message}")
            } finally {
                // Emit connection status: server stopped
                _connectionStatus.emit(uuid to false)
                stopServer()
            }
        }
    }

    private fun handleClient(
        clientId: String,
        onMessageReceived: (String, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = connections[clientId]?.reader
                while (true) {
                    val message = reader?.readLine() ?: break
                    onMessageReceived(clientId, message) // Pass the client UUID and message
                    Log.d("SocketService", "Received message from $clientId: $message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("SocketService", "Error occurred while reading message from $clientId: ${e.message}")
            } finally {
                // Emit connection status: client disconnected
                _connectionStatus.emit(clientId to false)
                disconnect(clientId)
            }
        }
    }

    fun sendMessage(uuid: String, message: String) {
        scope.launch(Dispatchers.IO) {
            val connection = connections[uuid]
            connection?.writer?.println(message)
            Log.d("SocketService", "Sent message to $uuid: $message")
        }
    }

    fun disconnect(uuid: String) {
        scope.launch(Dispatchers.IO) {
            val connection = connections[uuid]
            connection?.let {
                it.writer.close()
                it.reader.close()
                it.socket.close()
            }
            connections.remove(uuid)

            // Emit connection status: disconnected
            _connectionStatus.emit(uuid to false)
            Log.d("SocketService", "Disconnected from $uuid")
        }
    }

    fun stopServer() {
        scope.launch(Dispatchers.IO) {
            connections.values.forEach { connection ->
                connection.writer.close()
                connection.reader.close()
                connection.socket.close()
            }
            connections.clear()
            serverSocket?.close()

            // Emit connection status: server stopped
            _connectionStatus.emit("0" to false)
            Log.d("SocketService", "Server stopped")
        }
    }
}