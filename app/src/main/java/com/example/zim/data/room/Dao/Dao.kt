package com.example.zim.data.room.Dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.zim.data.room.models.Users
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao{
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(users:Users)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(user:Users)

    @Delete
    suspend fun delete(users:Users)

    @Query("""
        SELECT * 
        FROM Users as U INNER JOIN Current_User as Crr
        ON Crr.User_ID = U.User_ID
    """)
    fun getCrrUser():Flow<Users>

    @Query("""
        SELECT *
        FROM Users as U 
        INNER JOIN Connections as Con 
        INNER JOIN Current_User as Crr
        WHERE (Con.UserA_ID = Crr.User_ID AND Con.UserB_ID = U.User_ID) 
        OR (Con.UserB_ID = Crr.User_ID AND Con.UserA_ID = U.User_ID)
    """)
    fun getConUsers():Flow<List<Users>>
}

@Dao
interface Messages{
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(messages: Messages)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(messages: Messages)

    @Delete
    suspend fun delete(messages: Messages)
}


