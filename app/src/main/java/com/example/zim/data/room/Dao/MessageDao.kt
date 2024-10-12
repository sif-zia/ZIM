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

@Dao
interface MessageDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertSentMessage(sentMessage: SentMessages)
//
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun updateSentMessage(sentMessage: SentMessages)
//
//    @Delete
//    suspend fun deleteSentMessage(sentMessage: SentMessages)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertReceivedMessage(receivedMessages: ReceivedMessages)
//
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun updateReceivedMessage(receivedMessages: ReceivedMessages)
//
//    @Delete
//    suspend fun deleteReceivedMessage(receivedMessages: ReceivedMessages)
//
//    @Transaction
//    @Query(
//        """
//        SELECT *
//        FROM received_messages
//        WHERE User_ID_FK = :userID
//    """
//    )
//    suspend fun getReceivedMessagesFromAUser(userID: Int): List<MessagesWithReceivedMessages>
//
//    @Transaction
//    @Query(
//        """
//        SELECT *
//        FROM sent_messages
//        WHERE User_ID_FK = :userID
//    """
//    )
//    suspend fun getSentMessagesToAUser(userID: Int): List<MessagesWithSentMessages>
//
//    @Transaction
//    @Query("""
//        SELECT M.*
//        FROM messages as M
//        INNER JOIN Received_Messages as R
//        ON M.Message_ID = R.Message_ID_FK
//        WHERE  R.User_ID_FK= :userID
//
//        UNION
//
//        SELECT M.*
//        FROM messages as M
//        INNER JOIN Sent_Messages as S
//        ON M.Message_ID = S.Message_ID_FK
//        WHERE  S.User_ID_FK= :userID
//    """)
//    suspend fun getAllMessagesOfAUser(userID: Int): List<Messages>

}