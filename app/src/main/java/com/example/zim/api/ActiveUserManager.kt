package com.example.zim.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages active users across the application
 * Provides shared state between client and server repositories
 */
@Singleton
class ActiveUserManager @Inject constructor() {
    // Concurrent map to store active users (publicKey -> ipAddress)
    private val _activeUserMap = ConcurrentHashMap<String, String>()

    // StateFlow to notify subscribers when the map changes
    private val _activeUsersFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val activeUsers: StateFlow<Map<String, String>> = _activeUsersFlow.asStateFlow()

    // Add user to active map
    fun addUser(publicKey: String, ipAddress: String) {
        _activeUserMap[publicKey] = ipAddress
        updateFlow()
        Log.d("ActiveUserManager", "Added user to active map: $publicKey -> $ipAddress")
    }

    // Remove user from active map
    fun removeUser(publicKey: String) {
        _activeUserMap.remove(publicKey)
        updateFlow()
        Log.d("ActiveUserManager", "Removed user from active map: $publicKey")
    }

    // Get IP address for a public key
    fun getIpAddressForUser(publicKey: String): String? {
        return _activeUserMap[publicKey]
    }

    // Check if user exists
    fun hasUser(publicKey: String): Boolean {
        return _activeUserMap.containsKey(publicKey)
    }

    // Get all active users
    fun getAllActiveUsers(): Map<String, String> {
        return _activeUserMap.toMap()
    }

    // Clear all users (useful when stopping server)
    fun clearAllUsers() {
        _activeUserMap.clear()
        updateFlow()
        Log.d("ActiveUserManager", "Cleared all active users")
    }

    // Update the StateFlow with the current map state
    private fun updateFlow() {
        _activeUsersFlow.value = _activeUserMap.toMap()
    }
}