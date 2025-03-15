package com.example.zim.batman

import android.util.Log
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.ClientRepository
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.utils.LogType
import com.example.zim.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatmanProtocol @Inject constructor(
    private val clientRepository: ClientRepository,
    private val activeUserManager: ActiveUserManager,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val logger: Logger
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var deviceId = ""
    private val sequenceNumber = AtomicInteger(0)

    // Replace Channel with ConcurrentLinkedQueue
    private val messageQueue = ConcurrentLinkedQueue<MessagePayload>()

    companion object {
        private const val DEFAULT_TTL = 50
        private const val OGM_INTERVAL = 1000L // 1 second
        private const val SLIDING_WINDOW_SIZE = 128
        private const val MSG_EXPIRE_TIME = 60000L // 1 minute expiration
        private const val CLEANUP_INTERVAL = 30000L // 30 seconds
        private const val QUEUE_PROCESSING_INTERVAL = 100L // 100ms
        private const val TAG = "BatmanProtocol"
    }

    // Maps originator to a map of neighbors and their OGM reception statistics
    private val originatorTable =
        ConcurrentHashMap<String, ConcurrentHashMap<String, OriginatorStats>>()

    // Maps destination to best next hop
    val routingTable = ConcurrentHashMap<String, String>()

    private val _routedUsers = MutableStateFlow<Map<String, String>>(emptyMap())
    val routedUsers: StateFlow<Map<String, String>> = _routedUsers.asStateFlow()

    // Messages we've already seen to avoid reprocessing
    private val processedMessages = ConcurrentHashMap<String, Long>()

    // Store pending messages when route isn't established yet
    private val pendingMessages = ConcurrentHashMap<String, MutableList<MessagePayload>>()

    init {
        // Start OGM broadcasting
        CoroutineScope(Dispatchers.IO).launch {
            logger.addLog(TAG, "Starting OGM Broadcasting", LogType.INFO)
            broadcastOGM()
        }

        // Process outgoing messages from queue
        CoroutineScope(Dispatchers.IO).launch {
            logger.addLog(TAG, "Starting Outgoing Messages Processing", LogType.INFO)
            processOutgoingMessages()
        }

        // Periodic cleanup of processed messages cache
        CoroutineScope(Dispatchers.IO).launch {
            logger.addLog(TAG, "Starting Queue Cleanup", LogType.INFO)
            cleanupProcessedMessages()
        }

        // Initialize device ID
        CoroutineScope(Dispatchers.IO).launch {
            userDao.getCurrentUserFlow().collect { userWithCurrentUser ->
                if(userWithCurrentUser != null)
                    deviceId = userWithCurrentUser.users.UUID
            }
        }
    }

    data class OriginatorStats(
        val recentOGMs: LinkedList<Int> = LinkedList(), // Store recent sequence numbers
        var lastSequence: Int = 0,
        var count: Int = 0
    )

    // Generate and broadcast OGM periodically
    private suspend fun broadcastOGM() {
        while (true) {
            val noOfConnectedDevices = activeUserManager.getAllActiveUsers().size

            if(noOfConnectedDevices == 0) {
                delay(OGM_INTERVAL)
                continue
            }

            val ogm = OriginatorMessage(
                originatorAddress = deviceId,
                senderAddress = deviceId,
                sequenceNumber = sequenceNumber.incrementAndGet(),
                ttl = DEFAULT_TTL,
                originalTtl = DEFAULT_TTL
            )

            // Send OGM to all direct neighbors
            forwardOGM(ogm)
            logger.addLog(TAG, "OGM with sequence number ${ogm.sequenceNumber} broadcast", LogType.INFO)
            delay(OGM_INTERVAL)
        }
    }

    // Handle received OGM
    suspend fun processOGM(ogm: OriginatorMessage) {
        // Create key for checking if we've processed this OGM
        val messageKey = "${ogm.originatorAddress}:${ogm.sequenceNumber}"

        logger.addLog(TAG, "OGM with key ${messageKey.substring(0, 5)} Received", LogType.INFO)

        // Skip if we've already processed this message or it's our own returning to us
        if (processedMessages.containsKey(messageKey) || ogm.originatorAddress == deviceId) {
            return
        }

        // Mark as processed to avoid loops
        processedMessages[messageKey] = System.currentTimeMillis()

        // Update originator table
        updateOriginatorTable(ogm)

        // Recalculate best route for this originator
        updateBestRoute(ogm.originatorAddress)

        // Process any pending messages for this originator
        processPendingMessages(ogm.originatorAddress)

        // Forward OGM if TTL allows if there is no payload
        if (ogm.payload == null && ogm.ttl > 1) {
            val forwardOgm = ogm.copy(ttl = ogm.ttl - 1, senderAddress = deviceId)
            forwardOGM(forwardOgm)
        }

        // Process payload if present
        ogm.payload?.let { payload ->
            val payloadKey = "${payload.messageId}:${payload.sourceAddress}"
            if (processedMessages.containsKey(payloadKey)) {
                return@let  // Skip if we've already processed this payload
            }
            processedMessages[payloadKey] = System.currentTimeMillis()

            if (payload.destinationAddress == deviceId) {
                // Message is for us
                deliverMessage(payload)
            } else {
                // Forward message to next hop
                forwardMessage(payload)
            }
        }
    }

    private fun updateOriginatorTable(ogm: OriginatorMessage) {
        val originator = ogm.originatorAddress
        val neighborAddress = ogm.senderAddress

        // Get or create stats for this originator
        val originatorMap = originatorTable.getOrPut(originator) { ConcurrentHashMap() }

        // Get or create stats for this neighbor
        val stats = originatorMap.getOrPut(neighborAddress) { OriginatorStats() }

        // Only update if sequence number is newer or a reasonable replay
        if (ogm.sequenceNumber > stats.lastSequence ||
            (stats.lastSequence - ogm.sequenceNumber) > SLIDING_WINDOW_SIZE / 2
        ) {

            // Add to recent OGMs list
            stats.recentOGMs.addLast(ogm.sequenceNumber)

            // Maintain sliding window size
            if (stats.recentOGMs.size > SLIDING_WINDOW_SIZE) {
                stats.recentOGMs.removeFirst()
            }

            // Update stats
            stats.lastSequence = ogm.sequenceNumber
            stats.count = stats.recentOGMs.size
        }
    }

    private fun updateBestRoute(originatorAddress: String) {
        val originatorMap = originatorTable[originatorAddress] ?: return

        // Find neighbor with highest reception rate
        var bestNeighbor: String? = null
        var bestCount = 0

        originatorMap.forEach { (neighbor, stats) ->
            if (stats.count > bestCount) {
                bestCount = stats.count
                bestNeighbor = neighbor
            }
        }

        // Update routing table with best next hop
        bestNeighbor?.let {
            addRoute(originatorAddress, it)
        }
    }

    private suspend fun forwardOGM(ogm: OriginatorMessage) {
        // Get all neighbors except the one we received from
        activeUserManager.getAllActiveUsers().forEach { user ->
            if (user.key != ogm.senderAddress) {
                clientRepository.sendOGM(ogm, user.value)
            }
        }
    }

    // Send a message to a specific destination
    suspend fun sendMessage(destination: String, content: String) {
        val payload = MessagePayload(
            messageId = UUID.randomUUID().toString(),
            sourceAddress = deviceId,
            destinationAddress = destination,
            content = content
        )

        logger.addLog(TAG, "Adding message with ID ${payload.messageId.substring(0, 5)} to queue", LogType.INFO)

        // Add to message queue
        messageQueue.offer(payload)
    }

    private suspend fun processOutgoingMessages() {
        while (true) {
            // Process all messages in the queue
            while (messageQueue.isNotEmpty()) {
                val payload = messageQueue.poll() ?: continue

                // Mark this message as processed to avoid loops
                val payloadKey = "${payload.messageId}:${payload.sourceAddress}"
                processedMessages[payloadKey] = System.currentTimeMillis()

                val destination = payload.destinationAddress
                val nextHop = routingTable[destination]

                logger.addLog(TAG, "Processing message with ID ${payload.messageId.substring(0, 5)} to ${destination.substring(0, 5)}", LogType.INFO)

                if (nextHop != null) {
                    // Route exists, send message
                    sendViaNextHop(payload, nextHop)
                } else {
                    // No route, store message and send discovery OGM
                    storePendingMessage(destination, payload)
                    sendDiscoveryOGM(destination)
                }
            }
            delay(QUEUE_PROCESSING_INTERVAL)
        }
    }

    private suspend fun sendViaNextHop(payload: MessagePayload, nextHop: String) {
        val peer = activeUserManager.getIpAddressForUser(nextHop)
        if (peer != null) {
            // Create OGM with payload
            val ogm = OriginatorMessage(
                originatorAddress = deviceId,
                senderAddress = deviceId,
                sequenceNumber = sequenceNumber.incrementAndGet(),
                ttl = DEFAULT_TTL,
                originalTtl = DEFAULT_TTL,
                payload = payload
            )

            val payloadKey = "${payload.messageId}:${payload.sourceAddress}"
            processedMessages[payloadKey] = System.currentTimeMillis()

            val isSentOgm = clientRepository.sendOGM(ogm, peer)

            if (!isSentOgm) {
                storePendingMessage(payload.destinationAddress, payload)
            }
        } else {
            // Peer not directly connected, need to find another route
            removeRoute(payload.destinationAddress)
            storePendingMessage(payload.destinationAddress, payload)
        }
    }

    private fun storePendingMessage(destination: String, payload: MessagePayload) {
        // Check if we've already processed this message before adding to pending
        val payloadKey = "${payload.messageId}:${payload.sourceAddress}"
        if (!processedMessages.containsKey(payloadKey)) {
            pendingMessages.getOrPut(destination) { mutableListOf() }.add(payload)
        }
    }

    private suspend fun sendDiscoveryOGM(destination: String) {
        // Send an OGM with higher TTL to discover routes
        val ogm = OriginatorMessage(
            originatorAddress = deviceId,
            senderAddress = deviceId,
            sequenceNumber = sequenceNumber.incrementAndGet(),
            ttl = DEFAULT_TTL * 2,  // Higher TTL for discovery
            originalTtl = DEFAULT_TTL * 2
        )

        logger.addLog(TAG, "Sending Discovery OGM with sequence number ${ogm.sequenceNumber}", LogType.INFO)

        forwardOGM(ogm)
    }

    private suspend fun processPendingMessages(originator: String) {
        pendingMessages[originator]?.let { messages ->
            val nextHop = routingTable[originator]
            if (nextHop != null && messages.isNotEmpty()) {
                val messagesList = messages.toList() // Create a copy to avoid concurrent modification
                pendingMessages.remove(originator)

                messagesList.forEach { message ->
                    // Check if we've already processed this message
                    val payloadKey = "${message.messageId}:${message.sourceAddress}"
                    if (!processedMessages.containsKey(payloadKey)) {
                        sendViaNextHop(message, nextHop)
                        logger.addLog(TAG, "Sending pending message with ID ${message.messageId.substring(0, 5)} through ${nextHop.substring(0, 5)}", LogType.INFO)
                    }
                }
            }
        }
    }

    private fun deliverMessage(payload: MessagePayload) {
        // Message is for this device, deliver to application layer
        coroutineScope.launch {
            insertReceivedMessage(payload.sourceAddress, payload.content)
        }
        // Here you would integrate with your app's UI/notification system
    }

    private suspend fun forwardMessage(payload: MessagePayload) {
        val destination = payload.destinationAddress
        val nextHop = routingTable[destination]

        if (nextHop != null) {
            sendViaNextHop(payload, nextHop)
        } else {
            // No known route, store message
            storePendingMessage(destination, payload)
            // Send discovery OGM
            sendDiscoveryOGM(destination)
        }
    }

    private suspend fun cleanupProcessedMessages() {
        while (true) {
            val currentTime = System.currentTimeMillis()

            processedMessages.entries.removeIf { entry ->
                currentTime - entry.value > MSG_EXPIRE_TIME
            }

            delay(CLEANUP_INTERVAL) // Run cleanup every 30 seconds
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

    fun addRoute(destination: String, nextHop: String) {
        routingTable[destination] = nextHop
        updateFlow()
    }

    // Remove user from active map
    fun removeRoute(destination: String) {
        routingTable.remove(destination)
        updateFlow()
    }

    private fun updateFlow() {
        _routedUsers.value = routingTable.toMap()
    }
}