package com.example.zim.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.zim.data.room.Dao.AlertDao
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.converters.Converters
import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.GroupMemberships
import com.example.zim.data.room.models.GroupMsgReceivers
import com.example.zim.data.room.models.Groups
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.ReceivedAlerts
import com.example.zim.data.room.models.ReceivedMessages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.data.room.models.Users
import com.example.zim.data.room.schema.Schema

@Database(
    entities = [Users::class, CurrentUser::class, Messages::class, SentMessages::class, ReceivedMessages::class,
        Alerts::class, ReceivedAlerts::class, Groups::class, GroupMemberships::class, GroupMsgReceivers::class],
    version = 6,
)
@TypeConverters(Converters::class)
abstract class ZIMDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val messageDao: MessageDao
    abstract val alertDao: AlertDao

    companion object {
        @Volatile
        private var INSTANCE: ZIMDatabase? = null

        fun getInstance(context: Context): ZIMDatabase {
            synchronized(this) {
                if (INSTANCE == null) {
                    return Room.databaseBuilder(
                        context.applicationContext,
                        ZIMDatabase::class.java,
                        Schema.DB_NAME,
                    ).fallbackToDestructiveMigration().build()
                } else {
                    return INSTANCE as ZIMDatabase
                }
            }
        }

        //        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//
//            }
//
//        }
//        private val MIGRATION_2_1 = object : Migration(2, 1) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//
//            }
//
//        }
//        val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // Insert Users
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Itisam', 'Zia', '2002-12-06')")
//                db.execSQL("INSERT INTO Curr_User(User_ID_FK) VALUES (1)")
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Zainab', 'Bilal', '2002-09-25')")
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Muaaz', 'Aamer', '2002-05-10')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Ahmed', 'Khan', '2001-03-15')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Sara', 'Iqbal', '2003-07-25')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Ali', 'Raza', '2000-11-05')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Nadia', 'Javed', '2001-08-18')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Usman', 'Shah', '2004-01-30')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Maryam', 'Mujtaba', '2000-02-12')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Bilal', 'Mehmood', '2002-04-20')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Hassan', 'Ali', '2001-09-10')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Fatima', 'Khan', '2002-03-14')");
//                db.execSQL("INSERT INTO Users(fName, lName, DOB) VALUES ('Omer', 'Farooq', '2001-06-09')");
//
//                db.execSQL("INSERT INTO messages (msg, isSent, isDm) VALUES ('Hey Zainab, kya haal hai?', 1, 1)");
//                db.execSQL("INSERT INTO sent_messages (user_id_fk, message_id_fk, sentTime) VALUES (2, 1, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDm) VALUES ('Bas theek hoon, tum sunao?', 0, 1)");
//                db.execSQL("INSERT INTO received_messages (user_id_fk, message_id_fk, receivedTime) VALUES (2, 2, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDM) VALUES ('Main bhi theek hoon, aaj free ho?', 1, 1)");
//                db.execSQL("INSERT INTO sent_messages (user_id_fk, message_id_fk, sentTime) VALUES (2, 3, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDM) VALUES ('Haan free hoon, kuch plan hai?', 0, 1)");
//                db.execSQL("INSERT INTO received_messages (user_id_fk, message_id_fk, receivedTime) VALUES (2, 4, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDm) VALUES ('Chalo coffee pe chalte hain!', 1, 1)");
//                db.execSQL("INSERT INTO sent_messages (user_id_fk, message_id_fk, sentTime) VALUES (2, 5, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDm) VALUES ('Wah, achi idea hai! Kab nikalna hai?', 0, 1)");
//                db.execSQL("INSERT INTO received_messages (user_id_fk, message_id_fk, receivedTime) VALUES (2, 6, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDm) VALUES ('Ek ghante baad milte hain!', 1, 1)");
//                db.execSQL("INSERT INTO sent_messages (user_id_fk, message_id_fk, sentTime) VALUES (2, 7, CURRENT_TIMESTAMP)");
//                Thread.sleep(1000)
//                db.execSQL("INSERT INTO messages (msg, isSent, isDM) VALUES ('Perfect! Milte hain!', 0, 1)");
//                db.execSQL("INSERT INTO received_messages (user_id_fk, message_id_fk, receivedTime) VALUES (2, 8, CURRENT_TIMESTAMP)");
//            }
//
//
//        }

    }
}