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
            SELECT M.msg AS message, M.type AS type, R.receivedTime AS time, 1 AS isReceived
            FROM messages AS M
            INNER JOIN Received_Messages AS R
            ON M.Message_ID = R.Message_ID_FK
            WHERE  R.User_ID_FK= :userID
    
            UNION
    
            SELECT M.msg AS message, M.type AS type, S.sentTime AS time, 0 AS isReceived
            FROM messages AS M
            INNER JOIN Sent_Messages AS S
            ON M.Message_ID = S.Message_ID_FK
            WHERE  S.User_ID_FK= :userID
        )
        ORDER BY time
    """)
    fun getAllMessagesOfAUser(userID: Int): Flow<List<ChatContent>>

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
    suspend fun getUnReadMsgsCount(): Int

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
}