package com.example.zim.viewModels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.api.ClientRepository
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.events.ProtocolEvent
import com.example.zim.helperclasses.NewConnectionProtocol
import com.example.zim.utils.Package
import com.example.zim.repositories.SocketService
import com.example.zim.states.ProtocolState
import com.example.zim.utils.Crypto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Thread.yield
import java.time.LocalDateTime
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.math.log

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val application: Application,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val socketService: SocketService,
    private val clientRepository: ClientRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProtocolState())
    private var isServerRunning = false
    private val TAG = "Protocol"


    val state: StateFlow<ProtocolState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ProtocolState()

    )

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLocationEnabled = isLocationEnabled(),
                    isHotspotEnabled = isHotspotEnabled(),
                )
            }
        }

        initNewConnectionProtocol()
        observeSocketConnection()
    }

    fun onEvent(event: ProtocolEvent) {
        when (event) {
            is ProtocolEvent.LocationEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLocationEnabled = true) }
                }
            }

            is ProtocolEvent.LocationDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLocationEnabled = false) }
                }
            }

            is ProtocolEvent.WifiEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isWifiEnabled = true) }
                }
            }

            is ProtocolEvent.WifiDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isWifiEnabled = false) }
                }
            }

            is ProtocolEvent.HotspotEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isHotspotEnabled = true) }
                }
            }

            is ProtocolEvent.HotspotDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isHotspotEnabled = false) }
                }
            }

            is ProtocolEvent.LaunchEnableLocation -> {
                promptEnableLocation()
            }

            is ProtocolEvent.LaunchEnableWifi -> {
                promptEnableWifi()
            }

            is ProtocolEvent.LaunchEnableHotspot -> {
                promptEnableHotspot()
            }

            is ProtocolEvent.ChangeMyDeviceName -> {
                viewModelScope.launch {
                    userDao.setCurrUserDeviceName(event.newDeviceName)
                }
            }

            is ProtocolEvent.StartClient -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            groupOwnerIp = event.groupOwnerIp ?: "192.168.49.1",
                            amIGroupOwner = false
                        )
                    }
                    clientRepository.handshake(event.groupOwnerIp ?: "192.168.49.1")
                }
//                _state.value.newConnectionProtocol?.initUser(false)
//                connectToDefaultServer()
            }

            is ProtocolEvent.StartServer -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            groupOwnerIp = event.groupOwnerIp ?: "192.168.49.1",
                            amIGroupOwner = true
                        )
                    }
//                    _state.value.newConnectionProtocol?.initUser(true)
//                    socketService.start(
//                        SocketService.Mode.UpdateServer,
//                        "0",
//                        null,
//                        8888,
//                        ::onMessageReceive
//                    )
                }

            }

            is ProtocolEvent.InitServer -> {
                viewModelScope.launch {
                    startDefaultServer()

                }
            }

            is ProtocolEvent.SendMessage -> {
                if (event.message.isNotEmpty())
                    viewModelScope.launch {
                        sendMessage(event.id, event.message)
                    }
            }

            is ProtocolEvent.SendImage -> {
                sendImage(event.imageUri, event.userId)
                viewModelScope.launch {
                    insertSentMessage(event.userId, event.imageUri.toString(), "Image")
                }
            }

            is ProtocolEvent.AutoConnect -> {
                viewModelScope.launch {
                    val user = userDao.getUserById(event.userId)
                    if (_state.value.connectionStatues[user.UUID] == null || _state.value.connectionStatues[user.UUID] == false) {
                        if (ActivityCompat.checkSelfPermission(
                                application,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                                application,
                                Manifest.permission.NEARBY_WIFI_DEVICES
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(
                                application,
                                "Permission not granted",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@launch
                        }
                        _state.value.wifiP2pManager?.requestPeers(_state.value.wifiChannel) { peers ->
                            peers.deviceList.forEach { device ->
                                if (device.deviceName == user.deviceName) {

                                    val config = WifiP2pConfig()
                                    config.deviceAddress = device.deviceAddress
                                    _state.value.wifiP2pManager?.connect(
                                        _state.value.wifiChannel,
                                        config,
                                        object : WifiP2pManager.ActionListener {
                                            override fun onSuccess() {
                                                Toast.makeText(
                                                    application,
                                                    "Connection Request Sent",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            override fun onFailure(p0: Int) {
                                                Toast.makeText(
                                                    application,
                                                    "Connection Request Failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                        })


                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun promptEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableHotspot() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun isHotspotEnabled(): Boolean {
        return try {
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun initNewConnectionProtocol() {
        viewModelScope.launch {
            val crrUser = userDao.getCurrentUser()?.users
            if (crrUser != null) {
                val newConnectionProtocol =
                    NewConnectionProtocol(
                        crrUser,
                        onProtocolStart = ::onProtocolStart,
                        onProtocolEnd = ::onProtocolEnd,
                        onProtocolError = ::onProtocolError,
                        onExistingConnection = ::onExistingConnection,
                        sendProtocolMessage = ::sendProtocolMessage
                    )
                viewModelScope.launch {
                    _state.update {
                        it.copy(newConnectionProtocol = newConnectionProtocol)
                    }
                }
            }
        }
    }

    fun onProtocolStart() {
        viewModelScope.launch {
            Log.d(TAG, "Protocol started")

        }
    }

    fun onProtocolEnd(newUser: Users) {
        viewModelScope.launch {
            val id = userDao.insertUser(newUser)
            if (id > 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        "Connection Successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        "Failed to save user",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            closeDefaultConnection()
            onExistingConnection(newUser.UUID)
        }
    }

    fun onProtocolError(error: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(application, error, Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("Messages2", error)
        closeDefaultConnection()
    }

    fun onExistingConnection(uuid: String) {
//        closeDefaultConnection()
        if (_state.value.amIGroupOwner == true) {
            openCustomServer(uuid)
            Log.d(TAG, "Connection already exists")
        } else {
            connectToCustomServer(uuid)
            Log.d(TAG, "Connection already exists")
        }
    }

    fun sendProtocolMessage(stepNo: Int, message: String) {
        sendDefaultMessage(stepNo, message)
    }

    private fun insertReceivedMessage(uuid: String, message: String, msgType: String = "Text") {
        viewModelScope.launch {
            val secretKey = getSecretKey(uuid)
            val decryptedMessage = decryptMessage(message, secretKey)

            val msgId = messageDao.insertMessage(
                Messages(
                    msg = decryptedMessage,
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

    private suspend fun sendPendingMessages(uuid: String) {
        // Use a mutex to ensure thread safety when accessing the pending messages
        val mutex = kotlinx.coroutines.sync.Mutex()

        mutex.withLock {
            // Get pending messages within the lock to ensure consistency
            val messages = messageDao.getPendingMessages(uuid)
            val myUuid = _state.value.newConnectionProtocol?.currentUser?.UUID ?: "0"
            val secretKey = getSecretKey(uuid)

            try {
                // Process each message within the lock

                for (message in messages) {
                    delay(50)
//                    sendMessage(userDao.getIdByUUID(uuid), message)
                    // Add a small delay to prevent overwhelming the socket
                    val encodedMessage = encryptMessage(message, secretKey)

                    val pkg = Package(myUuid, uuid, myUuid, Package.Type.Text(encodedMessage))
                    socketService.sendPackage(pkg)


                    Log.d(TAG, "Sending message to ${uuid}")
                }

                // Mark messages as sent only if the loop completes successfully
                messageDao.markPendingMessagesAsSent(uuid)
            } catch (e: Exception) {
                Log.e("SocketService", "Error sending pending messages: ${e.message}", e)
                // If there's an error, we don't mark messages as sent
            }
        }
    }


    private fun observeSocketConnection() {
        viewModelScope.launch {
            socketService.connectionStatus.collect { (uuid, connected) ->

                _state.update {
                    it.copy(
                        connectionStatues = it.connectionStatues + (uuid to connected)
                    )
                }

                if (connected && uuid != "0") {
                    sendPendingMessages(uuid)
                }

                if (_state.value.amIGroupOwner == true) {
                    if (uuid == "0" && connected) {
                        Log.d(TAG, "Client connected")

                        startProtocol()
                    } else if (uuid != "0" && connected) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                application,
                                "Custom Client connected with $uuid",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                } else if (_state.value.amIGroupOwner == false) {
                    if (uuid == "0" && connected) {
                        Log.d(TAG, "Connected to server")
                        startProtocol()
                    } else if (uuid != "0" && connected) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                application,
                                "Connected to custom server with $uuid",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
            }
        }
    }

    fun checkConnectionStatus(uuid: String): Boolean {
        if (!_state.value.connectionStatues.containsKey(uuid)) {
            return false
        }
        return _state.value.connectionStatues[uuid] ?: false
    }

    private fun onProtocolMessageReceived(pkg: Package) {
        if (pkg.type is Package.Type.Protocol) {
            _state.value.newConnectionProtocol?.processStep(pkg.type)
        }

    }

    private fun onCustomMessageReceived(pkg: Package) {
        if (pkg.type is Package.Type.Text) {
            insertReceivedMessage(pkg.sender, pkg.type.msg)
        }
    }

    private fun startDefaultServer() {
        if (isServerRunning == false) {
            isServerRunning = true
            viewModelScope.launch {
                socketService.start(
                    SocketService.Mode.Server,
                    "0",
                    null,
                    8888,
                    ::onMessageReceive
                )
            }
        }
    }

    private fun connectToDefaultServer() {
        viewModelScope.launch {
            socketService.start(
                SocketService.Mode.Client,
                "0",
                _state.value.groupOwnerIp,
                8888,
                ::onMessageReceive
            )
        }
    }

    private fun closeDefaultConnection() {
        viewModelScope.launch {
            if (_state.value.amIGroupOwner != true)
                socketService.disconnect("0")
        }
    }

    private fun sendDefaultMessage(stepNo: Int, message: String) {
        viewModelScope.launch {
            val pkg = Package(
                sender = _state.value.newConnectionProtocol?.currentUser?.UUID ?: "0",
                receiver = "0",
                carrier = _state.value.newConnectionProtocol?.currentUser?.UUID ?: "0",
                type = Package.Type.Protocol(stepNo, message)
            )
            socketService.sendPackage(pkg)
        }
    }

    private fun startProtocol() {
        viewModelScope.launch {
            val uuids = userDao.getUUIDs()
            _state.value.newConnectionProtocol?.startProtocol(uuids)
        }
    }

    private fun onImageMessageReceived(pkg: Package) {
        if (pkg.type is Package.Type.Image) {
            _state.update { state ->
                val currentChunks = state.imageArrays[pkg.type.imageHash] ?: emptyList()
                state.copy(
                    imageArrays = state.imageArrays + (pkg.type.imageHash to (currentChunks + pkg.type))
                )
            }

            val receivedChunks = _state.value.imageArrays[pkg.type.imageHash] ?: emptyList()
            val receivedImageExtension = pkg.type.imageType

            if (receivedChunks.size == pkg.type.totalChunks) {
                // Sort chunks by chunk number
                val sortedChunks = receivedChunks.sortedBy { it.chunkNo }

                // Merge chunks into a single ByteArray
                val fullImageData =
                    sortedChunks.fold(ByteArrayOutputStream()) { outputStream, chunk ->
                        outputStream.apply { write(chunk.chunk) }
                    }.toByteArray()

                if (fullImageData.contentHashCode().toString() == pkg.type.imageHash) {
                    val fullImageUri = saveImageOnDisk(fullImageData, receivedImageExtension)
                    insertReceivedMessage(pkg.sender, fullImageUri.toString(), "Image")

                }
                // Clear the chunks from state after successful processing
                _state.update { state ->
                    state.copy(imageArrays = state.imageArrays - pkg.type.imageHash)
                }
            }

        }
    }

    /**
     * Saves an image byte array to the device's storage using a chunked approach
     * @param imageData The byte array containing the image data
     * @param fileExtension The file extension of the image (e.g., "jpg", "png")
     * @return The content URI of the saved image
     */
    private fun saveImageOnDisk(imageData: ByteArray, fileExtension: String = "jpg"): Uri? {
        return try {
            // Create a safe file extension (remove any dots and ensure lowercase)
            val safeExtension = fileExtension.replace(".", "").lowercase()

            // Generate a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val filename = "IMG_${timestamp}.$safeExtension"

            // Get the app's private pictures directory
            val imagesDir = File(
                application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "received_images"
            )
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Create the file
            val imageFile = File(imagesDir, filename)

            // Write the image data in chunks to avoid OutOfMemory errors with large images
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

            // Create a content URI for the saved image using FileProvider
            val imageUri = FileProvider.getUriForFile(
                application,
                "${application.packageName}.fileprovider",
                imageFile
            )

            imageUri
        } catch (e: Exception) {
            Log.e("ImageReceiver", "Failed to save image", e)
            null // Return null if saving fails
        }
    }

    private fun onMessageReceive(pkg: Package) {

        if (pkg.type is Package.Type.Protocol)
            onProtocolMessageReceived(pkg)
        else if (pkg.type is Package.Type.Text)
            onCustomMessageReceived(pkg)
    }

    private fun openCustomServer(uuid: String) {
        viewModelScope.launch {
//            val port: Int = uuid.substring(0, 4).toInt(16)
//            socketService.start(SocketService.Mode.Server, uuid, null, port, ::onCustomMessageReceived)
            socketService.start(
                SocketService.Mode.UpdateServer,
                uuid,
                null,
                8888,
                ::onMessageReceive
            )


        }
    }

    private fun connectToCustomServer(uuid: String) {
        viewModelScope.launch {
            delay(500)
//            val port = _state.value.newConnectionProtocol?.currentUser?.UUID?.substring(0, 4)?.toInt(16) ?: 8888
            socketService.start(
                SocketService.Mode.Client,
                uuid,
                _state.value.groupOwnerIp,
                8888,
                ::onMessageReceive
            )
        }
    }

    private fun closeCustomConnection(uuid: String) {
        viewModelScope.launch {
            socketService.disconnect(uuid)
        }
    }

    private suspend fun getSecretKey(receiverUuid: String): SecretKeySpec {

        val crypto = Crypto()
        val privateKeyStr = userDao.getCurrentUser().currentUser.prKey ?: ""
        val privateKey = crypto.decodePrivateKey(privateKeyStr)
        val publicKey = crypto.decodePublicKey(receiverUuid)
        val sharedSecret = crypto.generateSharedSecret(privateKey, publicKey)
        val secretKey = crypto.deriveAESKey(sharedSecret)
        return secretKey

    }

    private fun encryptMessage(message: String, secretKey: SecretKeySpec): String {
        val crypto = Crypto()

        val encryptedMessage = crypto.encryptText(message, secretKey)
        val encodedMessage = crypto.encodeEncryptedData(encryptedMessage)

        return encodedMessage
    }

    private fun decryptMessage(message: String, secretKey: SecretKeySpec): String {
        val crypto = Crypto()

        val decodedMessage = crypto.decodeEncryptedData(message)
        val decryptedMessages = crypto.decryptText(decodedMessage, secretKey)

        return decryptedMessages
    }

    private fun sendMessage(id: Int, message: String) {
        viewModelScope.launch {
            val user = userDao.getUserById(id)
            val myUuid = _state.value.newConnectionProtocol?.currentUser?.UUID ?: "0"

            val secretKey = getSecretKey(user.UUID)
            val encodedMessage = encryptMessage(message, secretKey)

            val pkg = Package(myUuid, user.UUID, myUuid, Package.Type.Text(encodedMessage))
            socketService.sendPackage(pkg)
            Log.d(TAG, "Sending message to ${user.UUID}")
            insertSentMessage(id, message)
        }
    }

    private suspend fun insertSentMessage(userId: Int, message: String, msgType: String = "Text") {
        val uuid = userDao.getUserById(userId).UUID
        val status =
            if (_state.value.connectionStatues.containsKey(uuid) && _state.value.connectionStatues[uuid] == true) "Sent" else "Sending"

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

    fun initWifiManager(wifiP2pManager: WifiP2pManager, channel: Channel) {
        _state.update {
            it.copy(wifiP2pManager = wifiP2pManager, wifiChannel = channel)
        }
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = application.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val bufferSize = 1024
                val data = ByteArray(bufferSize)
                var bytesRead: Int
                while (stream.read(data, 0, bufferSize).also { bytesRead = it } != -1) {
                    buffer.write(data, 0, bytesRead)
                }
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getImageType(uri: Uri): String? {
        val contentResolver = application.contentResolver
        val mimeType = contentResolver.getType(uri)  // Get MIME type
        return mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    }

    private fun sendImage(uri: Uri, userId: Int) {
        viewModelScope.launch {
            val uuid = userDao.getUserById(userId).UUID
            val imageByteArray = uriToByteArray(uri)
            val imageType = getImageType(uri)
            val imageHash = imageByteArray.contentHashCode().toString()
            val myUuid = _state.value.newConnectionProtocol?.currentUser?.UUID ?: "0"

            if (imageByteArray != null && imageType != null) {
                val chunkSize = 1024  // Each chunk is 1024 bytes
                val totalChunks =
                    (imageByteArray.size + chunkSize - 1) / chunkSize  // Calculate total chunks

                for (i in 0 until totalChunks) {
                    val start = i * chunkSize
                    val end = minOf(start + chunkSize, imageByteArray.size)
                    val chunk = imageByteArray.copyOfRange(start, end)  // Extract chunk

                    val imageChunk = Package.Type.Image(
                        chunkNo = i,
                        totalChunks = totalChunks,
                        imageHash = imageHash,
                        chunk = chunk,
                        imageType = imageType
                    )

                    val pkg = Package(myUuid, uuid, myUuid, imageChunk)
                    socketService.sendPackage(pkg)

                    Log.d("SocketService", "Sent chunk ${i + 1}/$totalChunks of type: $imageType")
                }
            } else {
                Log.e("SocketService", "Failed to convert URI to byte array or get image type")
            }

        }

    }


}