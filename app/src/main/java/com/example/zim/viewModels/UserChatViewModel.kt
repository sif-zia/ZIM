package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.events.UserChatEvent
import com.example.zim.helperclasses.ChatBox
import com.example.zim.repositories.SocketRepository
import com.example.zim.states.UserChatState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
@HiltViewModel
class UserChatViewModel @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val socketRepository: SocketRepository,
    private val application: Application
) : ViewModel() {
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: Channel? = null

    private val _state = MutableStateFlow(UserChatState())
    val state: StateFlow<UserChatState> = _state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), UserChatState()
    )

    init {
        observeMessages()
        observeConnectionStatus()
    }

    @SuppressLint("MissingPermission")
    fun onEvent(event: UserChatEvent) {
        when (event) {
            is UserChatEvent.LoadData -> {
                viewModelScope.launch {
                    val user = userDao.getUserById(event.userId)
                    _state.update {
                        it.copy(
                            username = "${user.fName} ${user.lName}",
                            dpUri = user.cover,
                            userId = user.id,
                            uuid = user.UUID,
                            connected = _state.value.connectionStatuses[user.UUID] ?: false
                        )
                    }
                    loadChats()
                }
            }

            is UserChatEvent.SendMessage -> {
                if(event.message.isNotEmpty())
                    sendMessage(event.message)
            }

            is UserChatEvent.ReadAllMessages -> {
                viewModelScope.launch {
                    messageDao.readAllMessages(event.userId)
                }
            }

            is UserChatEvent.ConnectToUser -> {

            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            socketRepository.messages.collect { message ->
                if (isProtocolRunning()) {
                    protocolStep(message.second)
                } else {
                    _state.update {
                        it.copy(
                            messages = it.messages + ChatBox.ReceivedMessage(
                                message.second, LocalTime.now(), LocalDate.now()
                            )
                        )
                    }
                    insertReceivedMessage(_state.value.uuid to message.second)
                    loadChats()
                }
            }
        }
    }

    fun startServer(port: Int, uuid: String = "0") {
        viewModelScope.launch {
            loadMyData()
            socketRepository.startServer(uuid, port)
        }
    }

    fun connectToServer(ip: String, port: Int, uuid: String = "0") {
        viewModelScope.launch {
            loadMyData()
            socketRepository.connectToServer(uuid, ip, port)
        }
    }

    private fun sendMessage(message: String, isProtocolMessage: Boolean = false) {
        viewModelScope.launch {
            if (!isProtocolMessage) {
                _state.update {
                    it.copy(
                        messages = it.messages + ChatBox.SentMessage(
                            message, LocalTime.now(), LocalDate.now()
                        )
                    )
                }
                insertSentMessage(_state.value.uuid to message)
                socketRepository.sendMessage(_state.value.uuid, message)
                loadChats()
            } else {
                socketRepository.sendMessage("0", message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            socketRepository.closeAll() // Clean up resources
        }
    }

    fun makeMeHost() {
        _state.update { it.copy(amIHost = true) }
    }

    @SuppressLint("MissingPermission")
    private fun protocolStep(data: String = "") {
        if (!_state.value.amIHost) {
            when (_state.value.protocolStepNumber) {
                0 -> { // Receive Hello
                    if (data.substringAfter("0: ") == "Hello") {
                        incProtocolNumber()
                        Log.d("Messages2", "0) Received Hello")
                        protocolStep()
                    } else {
                        crashConnection()
                    }
                }

                1 -> { // Send Hello Back
                    sendMessage("1: Hello back", true)
                    incProtocolNumber()
                    Log.d("Messages2", "1) Sent Hello back")
                }

                2 -> { // Receive UUID
                    val UUID = data.substringAfter("2: ")
                    if (UUID.length != 36) {
                        Toast.makeText(
                            application, "Invalid UUID", Toast.LENGTH_SHORT
                        ).show()
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        val UUIDs = userDao.getUUIDs()
                        if (UUIDs.isNotEmpty() && UUID in UUIDs) {
                            Toast.makeText(
                                application, "Connection Already Exist", Toast.LENGTH_SHORT
                            ).show()

                            // Connect with chat connection
                            val ip = "192.168.49.1"
                            val uuid = _state.value.myData.UUID.toString()
                            val port = uuid.substring(0, 4).toInt(16)
                            Toast.makeText(
                                application, "Connecting to new $port Port", Toast.LENGTH_SHORT
                            ).show()
                            incProtocolNumber()
                            protocolStep()
                            viewModelScope.launch {
                                delay(1000)
                                connectToServer(ip, port, UUID)
                            }

                            // Close Previous Connection
                            closeDefaultConnection()
                        } else {
                            incProtocolNumber()
                            _state.update {
                                it.copy(
                                    connectionMetadata = it.connectionMetadata.copy(
                                        UUID = UUID
                                    ), isNewConnection = true
                                )
                            }
                            Log.d("Messages2", "2) Received UUID")
                            protocolStep()
                        }
                    }
                }

                3 -> { // Send UUID
                    sendMessage("3: ${_state.value.myData.UUID}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "3) Sent UUID")
                }

                4 -> { // Receive fName
                    val fName = data.substringAfter("4: ")
                    if (fName.isEmpty()) {
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    fName = fName
                                )
                            )
                        }
                        incProtocolNumber()
                        Log.d("Messages2", "4) Received fName")
                        protocolStep()
                    }
                }

                5 -> { // Send fName
                    sendMessage("5: ${_state.value.myData.fName}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "5) Sent fName")
                }

                6 -> { // Receive lName
                    val lName = data.substringAfter("6: ")
                    if (lName.isEmpty()) {
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    lName = lName
                                )
                            )
                        }
                        incProtocolNumber()
                        Log.d("Messages2", "6) Received lName")
                        protocolStep()
                    }
                }

                7 -> { // Send lName
                    sendMessage("7: ${_state.value.myData.lName}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "7) Sent lName")
                }

                8 -> { // Receive Device Name
                    val deviceName: String = data.substringAfter("8: ")
                    if (deviceName != "null" && deviceName != "") viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    deviceName = deviceName
                                )
                            )
                        }
                    }
                    incProtocolNumber()
                    Log.d("Messages2", "8) Received Device Name")
                    protocolStep()
                }

                9 -> { // Send Device Name
                    viewModelScope.launch {
                        val deviceName = userDao.getCurrentUser()?.users?.deviceName ?: ""
                        if (deviceName == "") {
                            crashConnection()
                            Toast.makeText(
                                application, "Try Restarting Wifi", Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        sendMessage("9: $deviceName", true)
                        incProtocolNumber()
                        Log.d("Messages2", "9) Sent Device Name")
                    }
                }

                10 -> { // Receive Goodbye
                    if (data.substringAfter("10: ") == "Goodbye") {
                        incProtocolNumber()
                        Log.d("Messages2", "10) Received Goodbye")
                        viewModelScope.launch {
                            val fName = _state.value.connectionMetadata.fName ?: ""
                            val lName = _state.value.connectionMetadata.lName ?: ""
                            val UUID = _state.value.connectionMetadata.UUID ?: ""
                            val deviceName = _state.value.connectionMetadata.deviceName ?: ""
                            userDao.insertUser(
                                Users(
                                    fName = fName,
                                    lName = lName,
                                    UUID = UUID,
                                    deviceName = deviceName
                                )
                            )
                        }
                        Toast.makeText(
                            application, "Connection Successful", Toast.LENGTH_SHORT
                        ).show()
                        closeDefaultConnection()
                    } else {
                        crashConnection()
                    }
                }

                else -> {
                    crashConnection()
                }
            }
        } else {
            when (_state.value.protocolStepNumber) {
                0 -> { // Send Hello
                    sendMessage("0: Hello", true)
                    incProtocolNumber()
                    Log.d("Messages2", "0) Sent Hello")
                }

                1 -> { // Receive Hello back
                    if (data.substringAfter("1: ") == "Hello back") {
                        incProtocolNumber()
                        Log.d("Messages2", "1) Received Hello back")
                        protocolStep()
                    } else {
                        crashConnection()
                    }
                }

                2 -> { // Send UUID
                    sendMessage("2: ${_state.value.myData.UUID}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "2) Sent UUID")
                }

                3 -> { // Receive UUID
                    val UUID = data.substringAfter("3: ")
                    if (UUID.length != 36) {
                        Toast.makeText(
                            application, "Invalid UUID", Toast.LENGTH_SHORT
                        ).show()
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        val UUIDs = userDao.getUUIDs()
                        if (UUID in UUIDs) {
                            Toast.makeText(
                                application, "Connection Already Exist", Toast.LENGTH_SHORT
                            ).show()

                            // Close Previous Connection
                            closeDefaultConnection()

                            // Start New Connection for Chat
                            val port = UUID.substring(0, 4).toInt(16)
                            Toast.makeText(
                                application, "Starting a new $port Port", Toast.LENGTH_SHORT
                            ).show()
                            startServer(port, UUID)
                        } else {
                            _state.update {
                                it.copy(
                                    connectionMetadata = it.connectionMetadata.copy(
                                        UUID = UUID
                                    ), isNewConnection = true
                                )
                            }
                            incProtocolNumber()
                            Log.d("Messages2", "3) Received UUID")
                            protocolStep()
                        }
                    }
                }

                4 -> { // Send fName
                    sendMessage("4: ${_state.value.myData.fName}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "4) Sent fName")
                }

                5 -> { // Receive fName
                    val fName = data.substringAfter("5: ")
                    if (fName.isEmpty()) {
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    fName = fName
                                )
                            )
                        }
                        incProtocolNumber()
                        Log.d("Messages2", "5) Received fName")
                        protocolStep()
                    }
                }

                6 -> { // Send lName
                    sendMessage("6: ${_state.value.myData.lName}", true)
                    incProtocolNumber()
                    Log.d("Messages2", "6) Sent lName")
                }

                7 -> { // Receive lName
                    val lName = data.substringAfter("7: ")
                    if (lName.isEmpty()) {
                        crashConnection()
                        return
                    }
                    viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    lName = lName
                                )
                            )
                        }
                        incProtocolNumber()
                        Log.d("Messages2", "7) Received lName")
                        protocolStep()
                    }
                }

                8 -> { // Send Device Name
                    viewModelScope.launch {
                        val deviceName = userDao.getCurrentUser()?.users?.deviceName ?: ""
                        if (deviceName == "") {
                            crashConnection()
                            Toast.makeText(
                                application, "Try Restarting Wifi", Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        sendMessage("8: $deviceName", true)
                        incProtocolNumber()
                        Log.d("Messages2", "8) Sent Device Name")
                    }
                }

                9 -> { // Receive Device Name
                    val deviceName: String = data.substringAfter("9: ")
                    if (deviceName != "null" && deviceName != "") viewModelScope.launch {
                        _state.update {
                            it.copy(
                                connectionMetadata = it.connectionMetadata.copy(
                                    deviceName = deviceName
                                )
                            )
                        }
                    }
                    incProtocolNumber()
                    Log.d("Messages2", "9) Received Device Name")
                    protocolStep()
                }

                10 -> { // Send Goodbye
                    sendMessage("10: Goodbye", true)
                    incProtocolNumber()
                    Log.d("Messages2", "10) Sent Goodbye")
                    viewModelScope.launch {
                        val fName = _state.value.connectionMetadata.fName ?: ""
                        val lName = _state.value.connectionMetadata.lName ?: ""
                        val UUID = _state.value.connectionMetadata.UUID ?: ""
                        val deviceName = _state.value.connectionMetadata.deviceName ?: ""
                        userDao.insertUser(
                            Users(
                                fName = fName, lName = lName, UUID = UUID, deviceName = deviceName
                            )
                        )
                    }
                    Toast.makeText(
                        application, "Connection Successful", Toast.LENGTH_SHORT
                    ).show()
                    closeDefaultConnection()
                }

                else -> {
                    crashConnection()
                }
            }
        }
    }

    private fun incProtocolNumber() {
        _state.update { it.copy(protocolStepNumber = it.protocolStepNumber + 1) }
    }

    private fun crashConnection(uuid: String = "0") {
        Toast.makeText(application, "Connection Failed", Toast.LENGTH_SHORT).show()
        _state.update { it.copy(protocolStepNumber = -1) }
        viewModelScope.launch {
            socketRepository.closeConnection(uuid)
        }
    }

    private fun loadMyData() {
        viewModelScope.launch {
            val data = userDao.getMyData()
            data?.let {
                _state.update { it.copy(myData = data) }
            }
        }
    }

    private fun isProtocolRunning() = _state.value.protocolStepNumber in 0..10

    private fun startProtocol() {
        _state.update { it.copy(protocolStepNumber = 0) }

        if (_state.value.amIHost) protocolStep()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            socketRepository.connectionStatus.collect { connectionStatus ->
                if (connectionStatus.first == "0") {
                    when (connectionStatus.second) {
                        true -> {
                            startProtocol()
                            _state.update { it.copy(connected = true) }
                            Log.d("Messages2", "Connected to ${connectionStatus.first}")
                        }

                        false -> {
                            _state.update { it.copy(connected = false) }
                            Log.d("Messages2", "Disconnected from ${connectionStatus.first}")
                        }
                    }
                } else {
                    when (connectionStatus.second) {
                        true -> {
                            Log.d("Messages2", "Connected to ${connectionStatus.first}")
                        }

                        false -> {
                            Log.d("Messages2", "Disconnected from ${connectionStatus.first}")
                        }
                    }
                }
                _state.update { it.copy(connectionStatuses = _state.value.connectionStatuses + connectionStatus) }
            }
        }
    }

    private fun insertReceivedMessage(message: Pair<String, String>) {
        viewModelScope.launch {
            val messageId = messageDao.insertMessage(
                Messages(msg = message.second, isSent = false)
            )

            userDao.getIdByUUID(message.first).let { userId ->
                messageDao.insertReceivedMessage(
                    ReceivedMessages(
                        receivedTime = LocalDateTime.now(),
                        msgIDFK = messageId.toInt(),
                        userIDFK = userId
                    )
                )
            }

//            loadChats()
        }
    }

    private fun insertSentMessage(message: Pair<String, String>) {
        viewModelScope.launch {
            val messageId = messageDao.insertMessage(
                Messages(msg = message.second, isSent = true)
            )

            userDao.getIdByUUID(message.first).let { userId ->
                messageDao.insertSentMessage(
                    SentMessages(
                        sentTime = LocalDateTime.now(),
                        msgIDFK = messageId.toInt(),
                        userIDFK = userId
                    )
                )
            }

//            loadChats()
        }
    }

    fun initWifiP2p(wifiP2pManager: WifiP2pManager, channel: Channel) {
        this.wifiP2pManager = wifiP2pManager
        this.channel = channel
    }

    private fun closeDefaultConnection() {
        viewModelScope.launch {
            _state.update { it.copy(protocolStepNumber = -1, isNewConnection = false) }
            socketRepository.closeConnection("0")
            Toast.makeText(application, "Connection Closed", Toast.LENGTH_SHORT).show()
            if (_state.value.amIHost) socketRepository.closeServer("0")
        }
    }

    private suspend fun loadChats() {
        val chatBoxList = mutableListOf<ChatBox>()

        val userId = _state.value.userId
        if (userId != -1) {
            messageDao.getAllMessagesOfAUser(userId).collectLatest { chatContentList ->
                chatBoxList.clear()
                chatBoxList.addAll(chatContentList.map { chatContent ->
                    if (chatContent.isReceived) ChatBox.ReceivedMessage(
                        chatContent.message,
                        chatContent.time.toLocalTime(),
                        chatContent.time.toLocalDate()
                    )
                    else ChatBox.SentMessage(
                        chatContent.message,
                        chatContent.time.toLocalTime(),
                        chatContent.time.toLocalDate()
                    )
                })

                if (chatBoxList.isNotEmpty()) {
                    val firstMessage = chatBoxList[0]
                    if (firstMessage is ChatBox.SentMessage) chatBoxList.add(
                        0,
                        ChatBox.DateChip(firstMessage.date)
                    )
                    else if (firstMessage is ChatBox.ReceivedMessage) chatBoxList.add(
                        0,
                        ChatBox.DateChip(firstMessage.date)
                    )

                    for (index in 2..<chatBoxList.size) {
                        val currMessage = chatBoxList[index]
                        val prevMessage = chatBoxList[index - 1]

                        if (currMessage is ChatBox.ReceivedMessage && prevMessage is ChatBox.ReceivedMessage) {
                            currMessage.isFirst = false
                            if (currMessage.date.isAfter(prevMessage.date)) {
                                chatBoxList.add(
                                    index, ChatBox.DateChip(currMessage.date)
                                )
                            }
                        } else if (currMessage is ChatBox.SentMessage && prevMessage is ChatBox.SentMessage) {
                            currMessage.isFirst = false
                            if (currMessage.date.isAfter(prevMessage.date)) {
                                chatBoxList.add(
                                    index, ChatBox.DateChip(currMessage.date)
                                )
                            }
                        } else if (currMessage is ChatBox.ReceivedMessage && prevMessage is ChatBox.SentMessage) {
                            if (currMessage.date.isAfter(prevMessage.date)) {
                                chatBoxList.add(
                                    index, ChatBox.DateChip(currMessage.date)
                                )
                                currMessage.isFirst = false
                            }
                        } else if (currMessage is ChatBox.SentMessage && prevMessage is ChatBox.ReceivedMessage) {
                            if (currMessage.date.isAfter(prevMessage.date)) {
                                chatBoxList.add(
                                    index, ChatBox.DateChip(currMessage.date)
                                )
                                currMessage.isFirst = false
                            }
                        }
                    }
                }

                _state.update { it.copy(messages = chatBoxList) }
            }
        }

    }
}