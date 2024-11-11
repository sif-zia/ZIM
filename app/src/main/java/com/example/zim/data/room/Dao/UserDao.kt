package com.example.zim.data.room.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.data.room.models.UserWithCurrentUser
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

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
    suspend fun getCurrentUser(): UserWithCurrentUser?  // Return nullable UserWithCurrentUser to avoid issues if no result found

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

    @Query("""
    SELECT 
        user_info.User_ID,
        user_info.fName,
        user_info.lName,
        MAX(user_info.latest_message_time) AS latest_message_time,
        user_info.latest_message_content
    FROM (
            SELECT 
                u.User_ID,
                u.fName,
                u.lName,
                MAX(sm.sentTime) AS latest_message_time,
                m.msg AS latest_message_content
            FROM Users u
            LEFT JOIN Sent_Messages sm ON sm.User_ID_FK = u.User_ID
            LEFT JOIN Messages m ON m.Message_ID = sm.Message_ID_FK
            GROUP BY u.User_ID, u.fName, u.lName

            UNION

            SELECT 
                u.User_ID,
                u.fName,
                u.lName,
                MAX(rm.receivedTime) AS latest_message_time,
                m.msg AS latest_message_content
            FROM Users u
            LEFT JOIN Received_Messages rm ON rm.User_ID_FK = u.User_ID
            LEFT JOIN Messages m ON m.Message_ID = rm.Message_ID_FK
            GROUP BY u.User_ID, u.fName, u.lName
        ) AS user_info
    GROUP BY user_info.User_ID, user_info.fName, user_info.lName
    ORDER BY latest_message_time DESC
""")
    fun getUsersWithLatestMessage(): Flow<List<UserWithLatestMessage>>

}

data class UserWithLatestMessage(
    val User_ID: Int,
    val fName: String,
    val lName: String,
    val latest_message_time: LocalDateTime?,
    val latest_message_content: String?
)