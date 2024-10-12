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
    // Insert a User and return the generated ID
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: Users):Int // Returns the ID of the inserted user
//
//    // Update a User
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun updateUser(user: Users)
//
//    // Delete a User
//    @Delete
//    suspend fun deleteUser(user: Users)
//
//    // Insert a Current User and return the generated ID
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertCurrUser(currUser: CurrentUser):Long // Returns the ID of the inserted current user
//
//    // Update a Current User
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun updateCurrUser(currUser: CurrentUser)

//    // Get Current User with relation
//    @Transaction
//    @Query("""
//        SELECT *
//        FROM Users as U
//        INNER JOIN Curr_User as C
//        ON C.User_ID_FK = U.User_ID
//    """)
//    suspend fun getCurrentUser(): UserWithCurrentUser?  // Return nullable UserWithCurrentUser to avoid issues if no result found
//
//    // Get Connected Users
//    @Transaction
//    @Query("""
//        SELECT *
//        FROM Users as U
//        INNER JOIN Curr_User as C
//        ON C.User_ID_FK != U.User_ID
//    """)
//    suspend fun getConnectedUsers(): List<UserWithCurrentUser>  // Return a list of UserWithCurrentUser
}
