package com.example.zim.data.room.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.data.room.models.UserWithCurrentUser
import com.example.zim.helperclasses.Chat
import com.example.zim.helperclasses.ConnectionMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: Users): Long

    @Update
    suspend fun updateUser(user: Users)

    @Delete
    suspend fun deleteUser(user: Users)

    @Query("DELETE FROM Users WHERE User_ID = :id")
    suspend fun deleteUserById(id: Int): Int

    @Insert
    suspend fun insertCurrUser(currUser: CurrentUser): Long

    @Update
    suspend fun updateCurrUser(currUser: CurrentUser)

    // Get Current User with relation
    @Transaction
    @Query(
        """
        SELECT *
        FROM Users as U
        INNER JOIN Curr_User as C
        ON C.User_ID_FK = U.User_ID
        LIMIT 1
    """
    )
    suspend fun getCurrentUser(): UserWithCurrentUser? // Return nullable UserWithCurrentUser to avoid issues if no result found


    @Query(
        """
        SELECT *
        FROM Users as U
        INNER JOIN Curr_User as C
        ON C.User_ID_FK = U.User_ID
        LIMIT 1
    """
    )
    fun getCurrentUserFlow(): Flow<UserWithCurrentUser?>

    // Get Connected Users
    @Transaction
    @Query(
        """
        SELECT *
        FROM Users as U
        INNER JOIN Curr_User as C
        ON C.User_ID_FK != U.User_ID
    """
    )
    suspend fun getConnectedUsers(): List<UserWithCurrentUser>  // Return a list of UserWithCurrentUser

    @Transaction
    @Query(
        """
       SELECT EXISTS(
        SELECT *
        FROM Curr_User
    )
    """
    )
    suspend fun doesCurrentUserExist(): Boolean

    @Transaction
    @Query(
        """
    SELECT U.*
    FROM Users AS U
    INNER JOIN Curr_User AS C
    ON C.User_ID_FK != U.User_ID
    WHERE lName LIKE '%' || :name || '%'
    OR fName LIKE '%' || :name || '%'
"""
    )
    fun getUsersByName(name: String): Flow<List<Users>>

    @Transaction
    @Query("""
        SELECT *
        FROM Users AS U
        WHERE U.User_ID = :id
    """)
    suspend fun getUserById(id: Int): Users

    @Transaction
    @Query("""
    SELECT 
        user_info.User_ID AS id,
        user_info.fName,
        user_info.lName,
        user_info.UUID,
        MAX(user_info.latest_message_time) AS time,
        user_info.latest_message_content AS lastMsg,
        user_info.latest_message_type AS lastMsgType,
        user_info.latest_message_isSent AS isSent,
        SUM(user_info.unread_msgs) AS unReadMsgs
    FROM (
            SELECT 
                u.User_ID,
                u.fName,
                u.lName,
                u.UUID,
                MAX(sm.sentTime) AS latest_message_time,
                (SELECT msg FROM Messages WHERE Message_ID = sm.Message_ID_FK) AS latest_message_content,
                (SELECT type FROM Messages WHERE Message_ID = sm.Message_ID_FK) AS latest_message_type,
                (SELECT isSent FROM Messages WHERE Message_ID = sm.Message_ID_FK) AS latest_message_isSent,
                0 AS unread_msgs
            FROM Users u
            LEFT JOIN Sent_Messages sm ON sm.User_ID_FK = u.User_ID
            JOIN Curr_User cu ON cu.User_ID_FK != u.User_ID
            WHERE u.lName LIKE "%" || :query || "%" OR u.fName LIKE "%" || :query || "%"
            GROUP BY u.User_ID, u.fName, u.lName

            UNION ALL

            SELECT 
                u.User_ID,
                u.fName,
                u.lName,
                u.UUID,
                MAX(rm.receivedTime) AS latest_message_time,
                (SELECT msg FROM Messages WHERE Message_ID = rm.Message_ID_FK) AS latest_message_content,
                (SELECT type FROM Messages WHERE Message_ID = rm.Message_ID_FK) AS latest_message_type,
                (SELECT isSent FROM Messages WHERE Message_ID = rm.Message_ID_FK) AS latest_message_isSent,
                SUM(CASE WHEN rm.isRead = 0 THEN 1 ELSE 0 END) AS unread_msgs
            FROM Users u
            LEFT JOIN Received_Messages rm ON rm.User_ID_FK = u.User_ID
            JOIN Curr_User cu ON cu.User_ID_FK != u.User_ID
            WHERE u.lName LIKE "%" || :query || "%" OR u.fName LIKE "%" || :query || "%"
            GROUP BY u.User_ID, u.fName, u.lName
        ) AS user_info
    GROUP BY user_info.User_ID, user_info.fName, user_info.lName
    ORDER BY latest_message_time DESC
""")
    fun getUsersWithLatestMessage(query: String): Flow<List<Chat>>

    @Transaction
    @Query("""
        SELECT UUID
        FROM Users as U
        INNER JOIN Curr_User as C
        ON C.User_ID_FK != U.User_ID
    """)
    suspend fun getUUIDs(): List<String>

    @Transaction
    @Query("""
        SELECT U.UUID, U.fName, U.lName, U.deviceAddress
        FROM Users as U
        INNER JOIN Curr_User as C
        ON C.User_ID_FK == U.User_ID
        LIMIT 1
    """)
    suspend fun getMyData(): ConnectionMetadata?

    @Transaction
    @Query("SELECT User_ID FROM Users WHERE UUID = :uuid")
    suspend fun getIdByUUID(uuid: String): Int?


    @Query("UPDATE Users SET deviceAddress = :deviceAddress WHERE User_ID = (SELECT User_ID_FK FROM Curr_User LIMIT 1)")
    suspend fun setCurrUserDeviceAddress(deviceAddress: String)

    @Query("UPDATE Users SET deviceName = :deviceName WHERE User_ID = (SELECT User_ID_FK FROM Curr_User LIMIT 1)")
    suspend fun setCurrUserDeviceName(deviceName: String)
}