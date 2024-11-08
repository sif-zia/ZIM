package com.example.zim.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.converters.Converters
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.data.room.schema.Schema

@Database(
    entities = [
        Users::class,
        CurrentUser::class,
        Messages::class,
        SentMessages::class,
        ReceivedMessages::class
    ],
    version = 2,
)
@TypeConverters(Converters::class)
abstract class ZIMDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val messageDao: MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ZIMDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since no schema change affects existing data, we don’t need SQL here.
                // However, this migration will ensure Room recognizes the version update.
            }
        }

        fun getInstance(context: Context): ZIMDatabase {
            synchronized(this) {
                if (INSTANCE == null) {
                    return Room.databaseBuilder(
                        context.applicationContext,
                        ZIMDatabase::class.java,
                        Schema.DB_NAME,
                    ).addMigrations(MIGRATION_1_2)
                        .build()
                } else {
                    return INSTANCE as ZIMDatabase
                }
            }
        }
    }


}