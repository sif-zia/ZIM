package com.example.zim.repositories

import android.util.Log
import com.example.zim.utils.Package
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

class SocketService(private val scope: CoroutineScope) {

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private val connections = ConcurrentHashMap<String, Connection>()

    private val connectionsMutex = Mutex()

    // Connection timeout in milliseconds
    private val CONNECTION_TIMEOUT = 10000

    // UUID for default connections
    private var uuid: String? = null
    private var onPackageReceived: ((Package) -> Unit)? = null

    // SharedFlow to emit connection status (UUID to Boolean)
    private val _connectionStatus = MutableSharedFlow<Pair<String, Boolean>>(replay = 10)
    val connectionStatus: SharedFlow<Pair<String, Boolean>> get() = _connectionStatus

    // Data class to hold reader and writer for each connection
    private data class Connection(
        val socket: Socket,
        val reader: BufferedReader,
        val writer: PrintWriter,
        var job: Job? = null
    )

    sealed class Mode {
        object Client : Mode()
        object Server : Mode()
        object UpdateServer : Mode()
    }

    /**
     * Starts the socket service in client, server or update mode.
     */
    fun start(mode: Mode, uuid: String, host: String? = null, port: Int, onPackageReceived: (Package) -> Unit) {
        when (mode) {
            is Mode.Client -> connectAsClient(uuid, host ?: "192.168.49.1", port, onPackageReceived)
            is Mode.Server -> startAsServer(uuid, port, onPackageReceived)
            is Mode.UpdateServer -> updateServerParameters(uuid, onPackageReceived)
        }
    }

    private fun updateServerParameters(uuid: String, onPackageReceived: (Package) -> Unit) {
        this.uuid = uuid
        this.onPackageReceived = onPackageReceived
        Log.d("SocketService", "Updated server parameters with UUID: $uuid")
    }

    private fun connectAsClient(uuid: String, host: String, port: Int, onPackageReceived: (Package) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d("SocketService", "Attempting to connect to $host:$port with UUID: $uuid")

                // Create socket with timeout
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                // Store the connection
                connectionsMutex.withLock {
                    connections[uuid] = Connection(socket, reader, writer, coroutineContext.job)
                }

                // Emit connection status: connected
                _connectionStatus.emit(uuid to true)
                Log.d("SocketService", "Connected to $host:$port with UUID: $uuid")

                try {
                    while (isActive) {
                        val messageStr = reader.readLine() ?: break
                        val pkg = deserializePackage(messageStr)
                        onPackageReceived(pkg)
                        Log.d("SocketService", "Received package from $uuid: $messageStr")
                    }
                } catch (e: Exception) {
                    Log.e("SocketService", "Error reading from client $uuid: ${e.message}")
                } finally {
                    disconnect(uuid)
                }
            } catch (e: SocketTimeoutException) {
                Log.e("SocketService", "Connection timeout to $host:$port: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SocketService", "Error connecting to $host:$port: ${e.message}")
            } finally {
                // Emit connection status: disconnected
                _connectionStatus.emit(uuid to false)
                disconnect(uuid)
            }
        }
    }

    private fun handleClientConnection(clientSocket: Socket) {
        if (this.uuid == null || this.onPackageReceived == null) {
            Log.e("SocketService", "Server parameters not set")
            clientSocket.close()
            return
        }

        val clientId = this.uuid!!
        val packageHandler = this.onPackageReceived!!

        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = PrintWriter(clientSocket.getOutputStream(), true)

                // Store the connection
                connectionsMutex.withLock {
                    connections[clientId] = Connection(clientSocket, reader, writer, coroutineContext.job)
                }

                // Emit connection status: client connected
                _connectionStatus.emit(clientId to true)
                Log.d("SocketService", "Client connected with UUID: $clientId")

                try {
                    while (isActive) {
                        val messageStr = reader.readLine() ?: break
                        val pkg = deserializePackage(messageStr)
                        packageHandler(pkg)
                        Log.d("SocketService", "Received package from $clientId: $messageStr")
                    }
                } catch (e: Exception) {
                    Log.e("SocketService", "Error reading from client $clientId: ${e.message}")
                } finally {
                    disconnect(clientId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SocketService", "Error handling client connection: ${e.message}")
            } finally {
                // Emit connection status: client disconnected
                _connectionStatus.emit(clientId to false)
            }
        }
    }

    private fun startAsServer(uuid: String, port: Int, onPackageReceived: (Package) -> Unit) {
        this.uuid = uuid
        this.onPackageReceived = onPackageReceived

        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("SocketService", "Server started on port $port with UUID: $uuid")

                // Emit connection status: server started
                _connectionStatus.emit(uuid to true)

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleClientConnection(clientSocket)
                    } catch (e: SocketTimeoutException) {
                        // Timeout is expected, continue
                        continue
                    } catch (e: Exception) {
                        if (isActive && serverSocket?.isClosed == false) {
                            Log.e("SocketService", "Error accepting connection: ${e.message}")
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SocketService", "Error starting server on port $port: ${e.message}")
            } finally {
                // Emit connection status: server stopped
                _connectionStatus.emit(uuid to false)
                stopServer()
            }
        }
    }

    fun sendPackage(pkg: Package) {
        scope.launch(Dispatchers.IO) {
            try {
                val serializedPackage = serializePackage(pkg)

                connectionsMutex.withLock {
                    val connection = connections[pkg.receiver]
                    if (connection != null && !connection.socket.isClosed) {
                        connection.writer.println(serializedPackage)
                        Log.d("SocketService", "Sent package to ${pkg.receiver}: $serializedPackage")
                    } else {
                        Log.e("SocketService", "Failed to send package to ${pkg.receiver}: Connection not found or closed")
                        _connectionStatus.emit(pkg.receiver to false)
                    }
                }
            } catch (e: Exception) {
                Log.e("SocketService", "Error sending package to ${pkg.receiver}: ${e.message}")
                _connectionStatus.emit(pkg.receiver to false)
            }
        }
    }

    fun disconnect(uuid: String) {
        scope.launch(Dispatchers.IO) {
            connectionsMutex.withLock {
                val connection = connections[uuid]
                if (connection != null) {
                    try {
                        connection.job?.cancel()
                        connection.writer.close()
                        connection.reader.close()

                        if (!connection.socket.isClosed) {
                            connection.socket.close()
                        }

                        connections.remove(uuid)
                        Log.d("SocketService", "Disconnected from $uuid")
                    } catch (e: Exception) {
                        Log.e("SocketService", "Error disconnecting from $uuid: ${e.message}")
                    } finally {
                        _connectionStatus.emit(uuid to false)
                    }
                }
            }
        }
    }

    fun stopServer() {
        scope.launch(Dispatchers.IO) {
            try {
                connectionsMutex.withLock {
                    // Close all connections
                    connections.forEach { (uuid, connection) ->
                        try {
                            connection.job?.cancel()
                            connection.writer.close()
                            connection.reader.close()
                            if (!connection.socket.isClosed) {
                                connection.socket.close()
                            }
                            _connectionStatus.emit(uuid to false)
                        } catch (e: Exception) {
                            Log.e("SocketService", "Error closing connection to $uuid: ${e.message}")
                        }
                    }
                    connections.clear()

                    // Close server socket
                    if (serverSocket != null && !serverSocket!!.isClosed) {
                        serverSocket!!.close()
                    }
                    serverSocket = null

                    Log.d("SocketService", "Server stopped")
                }
            } catch (e: Exception) {
                Log.e("SocketService", "Error stopping server: ${e.message}")
            }
        }
    }

    // Serialization/Deserialization methods
    private fun serializePackage(pkg: Package): String {
        val json = JSONObject().apply {
            put("sender", pkg.sender)
            put("receiver", pkg.receiver)
            put("carrier", pkg.carrier)

            when (val type = pkg.type) {
                is Package.Type.Text -> {
                    put("type", "Text")
                    put("msg", type.msg)
                }
                is Package.Type.Protocol -> {
                    put("type", "Protocol")
                    put("stepNumber", type.stepNumber)
                    put("msg", type.msg)
                }

                else -> { put("type", "Other")}
            }

        }
        return json.toString()
    }

    private fun deserializePackage(jsonStr: String): Package {
        val json = JSONObject(jsonStr)
        val sender = json.getString("sender")
        val receiver = json.getString("receiver")
        val carrier = json.getString("carrier")

        val type: Package.Type = when (json.getString("type")) {
            "Text" -> Package.Type.Text(json.getString("msg"))
            "Protocol" -> Package.Type.Protocol(json.getInt("stepNumber"), json.getString("msg"))
            else -> throw IllegalArgumentException("Unknown package type")
        }

        return Package(sender, receiver, carrier, type)
    }
}