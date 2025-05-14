package com.example.zim.data.room.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.zim.api.MessageData
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.helperclasses.ChatContent
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
        SELECT *
        FROM received_messages
        WHERE User_ID_FK = :userID
    """
    )
    suspend fun getReceivedMessagesFromAUser(userID: Int): List<ReceivedMessages>

    @Transaction
    @Query(
        """
        SELECT *
        FROM sent_messages
        WHERE User_ID_FK = :userID
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
            WHERE  R.User_ID_FK= :userID
    
            UNION
    
            SELECT M.Message_ID AS id,  M.msg AS message, M.type AS type, S.sentTime AS time, 0 AS isReceived
            FROM messages AS M
            INNER JOIN Sent_Messages AS S
            ON M.Message_ID = S.Message_ID_FK
            WHERE  S.User_ID_FK= :userID
        )
        ORDER BY time
    """)
    fun getAllMessagesOfAUser(userID: Int): Flow<List<ChatContent>>

    @Transaction
    @Query("""
    SELECT M.*
    FROM Messages M
    JOIN Sent_Messages SM ON M.Message_ID = SM.Message_ID_FK
    WHERE SM.User_ID_FK = :userId
    UNION
    SELECT M.*
    FROM Messages M
    JOIN Received_Messages RM ON M.Message_ID = RM.Message_ID_FK
    WHERE RM.User_ID_FK = :userId
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
            SELECT MIN(isRead) AS isRead
            FROM Received_Messages
            GROUP BY User_ID_FK
        )
        WHERE isRead = 0
    """)
    fun getUnReadMsgsCount(): Flow<Int>

    @Query("""
        UPDATE Received_Messages
        SET isRead = 1
        WHERE User_ID_FK = :userId
    """)
    suspend fun readAllMessages(userId: Int)

    @Query("""
        SELECT Sent_Messages_ID AS messageId, M.msg AS content
        FROM Messages M 
        JOIN Sent_Messages SM ON M.Message_ID = SM.Message_ID_FK
        JOIN Users U ON SM.User_ID_FK = U.User_ID
        WHERE U.UUID = :receiverUuid AND SM.status= "Sending" AND M.type= "Text"
        ORDER BY SM.sentTime 
    """)
    suspend fun getPendingMessages(receiverUuid: String): List<MessageData>

    @Query("""
    UPDATE Sent_Messages
    SET status = "Sent"
    WHERE User_ID_FK IN (SELECT User_ID FROM Users WHERE UUID = :receiverUuid)
""")
    suspend fun markPendingMessagesAsSent(receiverUuid: String)

    @Query("""
        UPDATE Sent_Messages
        SET status = "Sent"
        WHERE Sent_Messages_ID = :sentMessageId
    """)
    suspend fun markMessageAsSent(sentMessageId: Int)

    @Query("UPDATE Sent_Messages SET status = :status WHERE Sent_Messages_ID IN (:messageIds)")
    suspend fun updateSentMessagesStatus(messageIds: List<Int>, status: String)

    @Query("""SELECT EXISTS(
        SELECT *
        FROM Received_Messages RM
        JOIN Users U ON RM.User_ID_FK = U.User_ID
        WHERE U.UUID = :senderUuid AND RM.receivedMessageId = :messageId
    )""")
    suspend fun checkMessageExist(senderUuid: String, messageId: Int): Boolean
}