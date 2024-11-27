package com.example.zim.data.room.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.MessagesWithReceivedMessages
import com.example.zim.data.room.models.MessagesWithSentMessages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.helperclasses.ChatContent
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insertSentMessage(sentMessage: SentMessages)

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
            SELECT M.msg AS message, R.receivedTime AS time, 1 AS isReceived
            FROM messages as M
            INNER JOIN Received_Messages as R
            ON M.Message_ID = R.Message_ID_FK
            WHERE  R.User_ID_FK= :userID
    
            UNION
    
            SELECT M.msg AS message, S.sentTime AS time, 0 AS isReceived
            FROM messages as M
            INNER JOIN Sent_Messages as S
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
}