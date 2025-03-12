package com.example.zim.di

import android.content.Context
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pManager
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.ZIMDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

    @Provides
    @Singleton
    fun provideWifiP2pManager(@ApplicationContext context: Context): WifiP2pManager {
        return context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
}