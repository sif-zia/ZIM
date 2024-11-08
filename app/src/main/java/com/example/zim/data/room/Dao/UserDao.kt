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
}
