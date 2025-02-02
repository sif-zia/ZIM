package com.example.zim.di

import android.content.Context
import androidx.activity.ComponentActivity
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.ZIMDatabase
import com.example.zim.wifiP2P.WifiP2pListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideZIMDatabase(@ApplicationContext context: Context): ZIMDatabase {
        return ZIMDatabase.getInstance(context)
    }

    @Provides
    fun provideUserDao(database: ZIMDatabase): UserDao {
        return database.userDao
    }

    @Provides
    fun provideMessageDao(database: ZIMDatabase): MessageDao {
        return database.messageDao
    }
}