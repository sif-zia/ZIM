package com.example.zim.data.room.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.zim.api.MessageData
import com.example.zim.data.room.models.GroupMsgReceivers
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.helperclasses.ChatContent
import com.example.zim.helperclasses.GroupChatContent
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(message: Messages): Long

    @Insert
    suspend fun insertSentMessage(sentMessage: SentMessages): Long

    @Update
    suspend fun updateSentMessage(sentMessage: SentMessages)

    @Delete
    suspend fun deleteSentMessage(sentMessage: SentMessages)

    @Insert
    suspend fun insertReceivedMessage(receivedMessages: ReceivedMessages)

    @Update
    suspend fun updateReceivedMessage(receivedMessages: ReceivedMessages)

    @Delete
    suspend fun deleteReceivedMessage(receivedMessages: ReceivedMessages)

    @Delete
    suspend fun deleteMessages(messages: List<Messages>)

    @Transaction
    suspend fun deleteMessagesAndReceivedMessages(messageIds: List<Int>) {
        // First delete all associated received messages
        deleteReceivedMessagesForMessageIds(messageIds)
        // Then delete the messages themselves
        deleteMessagesByIds(messageIds)
    }

    /**
     * Complete deletion of messages from all relevant tables:
     * 1. messages table
     * 2. received_messages table
     * 3. sent_messages table
     */
    @Transaction
    suspend fun deleteMessagesCompletely(messageIds: List<Int>) {
        // Delete from received_messages table
        deleteReceivedMessagesForMessageIds(messageIds)

        // Delete from sent_messages table
        deleteSentMessagesForMessageIds(messageIds)

        // Finally delete from messages table
        deleteMessagesByIds(messageIds)
    }

    @Query("DELETE FROM messages WHERE messages.Message_ID" +
            " IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<Int>)

    @Query("DELETE FROM RECEIVED_MESSAGES WHERE MESSAGE_ID_FK IN (:messageIds)")
    suspend fun deleteReceivedMessagesForMessageIds(messageIds: List<Int>)

    @Query("DELETE FROM SENT_MESSAGES WHERE MESSAGE_ID_FK IN (:messageIds)")
    suspend fun deleteSentMessagesForMessageIds(messageIds: List<Int>)


    @Transaction
    @Query(
        """
        SELECT RM.*
        FROM received_messages RM
        JOIN Messages M ON RM.Message_ID_FK = M.Message_ID
        WHERE RM.User_ID_FK = :userID AND M.isDM = 1
    """
    )
    suspend fun getReceivedMessagesFromAUser(userID: Int): List<ReceivedMessages>

    @Transaction
    @Query(
        """
        SELECT SM.*
        FROM sent_messages SM
        JOIN Messages M ON SM.Message_ID_FK = M.Message_ID
        WHERE SM.User_ID_FK = :userID AND M.isDM = 1
    """
    )
    suspend fun getSentMessagesToAUser(userID: Int): List<SentMessages>

    @Transaction
    @Query("""
        SELECT *
        FROM (
            SELECT M.Message_ID AS id, M.msg AS message, M.type AS type, R.receivedTime AS time, 1 AS isReceived
            FROM messages AS M
            INNER JOIN Received_Messages AS R
            ON M.Message_ID = R.Message_ID_FK
            WHERE R.User_ID_FK = :userID AND M.isDM = 1
    
            UNION
    
            SELECT M.Message_ID AS id, M.msg AS message, M.type AS type, S.sentTime AS time, 0 AS isReceived
            FROM messages AS M
            INNER JOIN Sent_Messages AS S
            ON M.Message_ID = S.Message_ID_FK
            WHERE S.User_ID_FK = :userID AND M.isDM = 1
        )
        ORDER BY time
    """)
    fun getAllMessagesOfAUser(userID: Int): Flow<List<ChatContent>>

    @Transaction
    @Query("""
    SELECT M.*
    FROM Messages M
    JOIN Sent_Messages SM ON M.Message_ID = SM.Message_ID_FK
    WHERE SM.User_ID_FK = :userId AND M.isDM = 1
    UNION
    SELECT M.*
    FROM Messages M
    JOIN Received_Messages RM ON M.Message_ID = RM.Message_ID_FK
    WHERE RM.User_ID_FK = :userId AND M.isDM = 1
""")
    suspend fun getAllMessagesOfUser(userId: Int): List<Messages>


    @Query("""
        SELECT *
        FROM Messages
        WHERE Message_ID = :id
        LIMIT 1
    """)
    suspend fun getMessageById(id: Int): Messages?

    @Query("""
        SELECT COUNT(*)
        FROM (
            SELECT MIN(RM.isRead) AS isRead
            FROM Received_Messages RM
            JOIN Messages M ON RM.Message_ID_FK = M.Message_ID
            WHERE M.isDM = 1
            GROUP BY RM.User_ID_FK
        )
        WHERE isRead = 0
    """)
    fun getUnReadMsgsCount(): Flow<Int>

    @Query("""
        UPDATE Received_Messages
        SET isRead = 1
        WHERE User_ID_FK = :userId
        AND Message_ID_FK IN (SELECT Message_ID FROM Messages WHERE isDM = 1)
    """)
    suspend fun readAllMessages(userId: Int)

    @Query("""
        SELECT Sent_Messages_ID AS messageId, M.msg AS content
        FROM Messages M 
        JOIN Sent_Messages SM ON M.Message_ID = SM.Message_ID_FK
        JOIN Users U ON SM.User_ID_FK = U.User_ID
        WHERE U.UUID = :receiverUuid AND SM.status = "Sending" AND M.type = "Text" AND M.isDM = 1
        ORDER BY SM.sentTime 
    """)
    suspend fun getPendingMessages(receiverUuid: String): List<MessageData>

    @Query("""
    UPDATE Sent_Messages
    SET status = "Sent"
    WHERE User_ID_FK IN (SELECT User_ID FROM Users WHERE UUID = :receiverUuid)
    AND Message_ID_FK IN (SELECT Message_ID FROM Messages WHERE isDM = 1)
""")
    suspend fun markPendingMessagesAsSent(receiverUuid: String)

    @Query("""
        UPDATE Sent_Messages
        SET status = "Sent"
        WHERE Sent_Messages_ID = :sentMessageId
        AND Message_ID_FK IN (SELECT Message_ID FROM Messages WHERE isDM = 1)
    """)
    suspend fun markMessageAsSent(sentMessageId: Int)

    @Query("""
        UPDATE Sent_Messages 
        SET status = :status 
        WHERE Sent_Messages_ID IN (:messageIds)
        AND Message_ID_FK IN (SELECT Message_ID FROM Messages WHERE isDM = 1)
    """)
    suspend fun updateSentMessagesStatus(messageIds: List<Int>, status: String)

    @Query("""
        SELECT EXISTS(
            SELECT *
            FROM Received_Messages RM
            JOIN Users U ON RM.User_ID_FK = U.User_ID
            JOIN Messages M ON RM.Message_ID_FK = M.Message_ID
            WHERE U.UUID = :senderUuid AND RM.receivedMessageId = :messageId AND M.isDM = 1
        )
    """)
    suspend fun checkMessageExist(senderUuid: String, messageId: Int): Boolean

    @Transaction
    @Query("""
    SELECT *
    FROM (
        -- Received messages in group
        SELECT 
            M.Message_ID AS id, 
            M.msg AS message, 
            M.type AS type, 
            R.receivedTime AS time, 
            1 AS isReceived,
            U.fName AS senderFName,
            U.lName AS senderLName
        FROM Messages AS M
        INNER JOIN Received_Messages AS R ON M.Message_ID = R.Message_ID_FK
        INNER JOIN Group_Message_Receivers AS GMR ON M.Message_ID = GMR.Message_ID_FK
        INNER JOIN Users AS U ON GMR.User_ID_FK = U.User_ID
        WHERE GMR.Group_ID_FK = :groupID AND M.isDM = 0
        
        UNION
        
        -- Sent messages in group
        SELECT 
            M.Message_ID AS id, 
            M.msg AS message, 
            M.type AS type, 
            S.sentTime AS time, 
            0 AS isReceived,
            CU.fName AS senderFName,
            CU.lName AS senderLName
        FROM Messages AS M
        INNER JOIN Sent_Messages AS S ON M.Message_ID = S.Message_ID_FK
        INNER JOIN Group_Message_Receivers AS GMR ON M.Message_ID = GMR.Message_ID_FK
        INNER JOIN Users AS CU ON S.User_ID_FK = CU.User_ID
        WHERE GMR.Group_ID_FK = :groupID AND M.isDM = 0
    )
    ORDER BY time
""")
    fun getGroupChatContent(groupID: Int): Flow<List<GroupChatContent>>

    /**
     * Insert a new entry in the GroupMsgReceivers table
     */
    @Insert
    suspend fun insertGroupMessageReceiver(groupMsgReceiver: GroupMsgReceivers): Long

    /**
     * Get all message IDs for a specific group
     */
    @Query("""
        SELECT DISTINCT M.Message_ID
        FROM Messages M
        JOIN Group_Message_Receivers GMR ON M.Message_ID = GMR.Message_ID_FK
        WHERE GMR.Group_ID_FK = :groupId AND M.isDM = 0
    """)
    suspend fun getGroupMessageIds(groupId: Int): List<Int>

    /**
     * Get all pending group messages for a specific group
     */
    @Query("""
        SELECT SM.Sent_Messages_ID AS messageId, M.msg AS content
        FROM Messages M 
        JOIN Sent_Messages SM ON M.Message_ID = SM.Message_ID_FK
        JOIN Group_Message_Receivers GMR ON M.Message_ID = GMR.Message_ID_FK
        WHERE GMR.Group_ID_FK = :groupId 
        AND SM.status = "Sending" 
        AND M.type = "Text" 
        AND M.isDM = 0
        ORDER BY SM.sentTime 
    """)
    suspend fun getPendingGroupMessages(groupId: Int): List<MessageData>

    /**
     * Mark all pending messages for a group as sent
     */
    @Query("""
        UPDATE Sent_Messages
        SET status = "Sent"
        WHERE Message_ID_FK IN (
            SELECT M.Message_ID
            FROM Messages M
            JOIN Group_Message_Receivers GMR ON M.Message_ID = GMR.Message_ID_FK
            WHERE GMR.Group_ID_FK = :groupId AND M.isDM = 0
        )
        AND status = "Sending"
    """)
    suspend fun markPendingGroupMessagesAsSent(groupId: Int)

    /**
     * Delete all messages for a specific group
     */
    @Transaction
    suspend fun deleteGroupMessages(groupId: Int) {
        val messageIds = getGroupMessageIds(groupId)
        if (messageIds.isNotEmpty()) {
            deleteMessagesCompletely(messageIds)
        }
    }

    /**
     * Mark all messages in a group as read for the current user
     */
    @Query("""
        UPDATE Received_Messages
        SET isRead = 1
        WHERE Message_ID_FK IN (
            SELECT M.Message_ID
            FROM Messages M
            JOIN Group_Message_Receivers GMR ON M.Message_ID = GMR.Message_ID_FK
            WHERE GMR.Group_ID_FK = :groupId AND M.isDM = 0
        )
        AND User_ID_FK = :userId
    """)
    suspend fun readAllGroupMessages(groupId: Int, userId: Int)

//    /**
//     * Get unread message count for each group
//     */
//    @Query("""
//        SELECT GMR.Group_ID_FK AS groupId, COUNT(*) AS unreadCount
//        FROM Received_Messages RM
//        JOIN Messages M ON RM.Message_ID_FK = M.Message_ID
//        JOIN Group_Message_Receivers GMR ON M.Message_ID = GMR.Message_ID_FK
//        WHERE RM.isRead = 0 AND M.isDM = 0 AND RM.User_ID_FK = :userId
//        GROUP BY GMR.Group_ID_FK
//    """)
//    fun getUnreadGroupMessageCounts(userId: Int): Flow<Map<Int, Int>>

    @Query("""
    SELECT EXISTS(
        SELECT *
        FROM Received_Messages RM
        JOIN Group_Message_Receivers GMR ON RM.Message_ID_FK = GMR.Message_ID_FK
        JOIN Messages M ON RM.Message_ID_FK = M.Message_ID
        WHERE GMR.Group_ID_FK = :groupId 
        AND GMR.User_ID_FK = (SELECT User_ID FROM Users WHERE UUID = :senderUuid)
        AND RM.receivedMessageId = :messageId
        AND M.isDM = 0
    )
""")
    suspend fun checkGroupMessageExist(groupId: Int, senderUuid: String, messageId: Int): Boolean
}