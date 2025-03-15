package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.ClientRepository
import com.example.zim.api.ServerRepository
import com.example.zim.batman.BatmanProtocol
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.ProtocolEvent
import com.example.zim.states.ProtocolState
import com.example.zim.wifiP2P.WifiDirectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val application: Application,
    private val userDao: UserDao,
    private val clientRepository: ClientRepository,
    private val activeUserManager: ActiveUserManager,
    private val wifiDirectManager: WifiDirectManager,
    private val batmanProtocol: BatmanProtocol,
    serverRepository: ServerRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProtocolState())

    val activeUsers = activeUserManager.activeUsers
    val routedUsers = batmanProtocol.routedUsers

    val state: StateFlow<ProtocolState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ProtocolState()
    )

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLocationEnabled = isLocationEnabled(),
                    isHotspotEnabled = isHotspotEnabled(),
                    isWifiEnabled = isWifiEnabled()
                )
            }
        }

        if (!serverRepository.isServerRunning.value)
            serverRepository.startServer()

        wifiDirectManager.addOnHotspotStateChangedCallback(::onHotspotStateChanged)
        wifiDirectManager.addOnLocationStateChangedCallback(::onLocationStateChanged)
        wifiDirectManager.addOnWifiStateChangedCallback(::onWifiStateChanged)
        wifiDirectManager.addOnConnectionCallback(::onConnection)
        wifiDirectManager.addOnDisconnectCallback(::onDisconnect)
        wifiDirectManager.addOnDeviceNameChangedCallback(::onDeviceNameChanged)
    }

    private fun onHotspotStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(isHotspotEnabled = enabled)
            }
        }
    }

    private fun onLocationStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(isLocationEnabled = enabled)
            }
        }
    }

    private fun onWifiStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(isWifiEnabled = enabled)
            }
        }
    }

    private fun onConnection(info: WifiP2pInfo, group: WifiP2pGroup) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    amIGroupOwner = info.isGroupOwner,
                    groupOwnerIp = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                )
            }
            if (info.groupFormed && !info.isGroupOwner) {
                clientRepository.handshake(info.groupOwnerAddress?.hostAddress ?: "192.168.49.1")
            }
        }
    }

    private fun onDisconnect() {
        viewModelScope.launch {
            activeUserManager.clearAllUsers()
            clientRepository.ips.clear()
            batmanProtocol.resetRouting()
        }
    }

    private fun onDeviceNameChanged(newName: String) {
        viewModelScope.launch {
            userDao.setCurrUserDeviceName(newName)
        }
    }

    @SuppressLint("MissingPermission")
    fun onEvent(event: ProtocolEvent) {
        when (event) {

            is ProtocolEvent.LaunchEnableLocation -> {
                promptEnableLocation()
            }

            is ProtocolEvent.LaunchEnableWifi -> {
                promptEnableWifi()
            }

            is ProtocolEvent.LaunchEnableHotspot -> {
                promptEnableHotspot()
            }

            is ProtocolEvent.SendMessage -> {
                if (event.message.isNotEmpty())
                    viewModelScope.launch {
//                        clientRepository.sendMessage(event.message, event.id)
                        val uuid= userDao.getUserById(event.id).UUID
                        batmanProtocol.sendMessage(uuid, event.message)

                    }
            }

            is ProtocolEvent.SendImage -> {
                val permanentImageUri = savePermanentImage(application, event.imageUri)
                if (permanentImageUri == null) {
                    Toast.makeText(application, "Send Failed.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModelScope.launch {
                        clientRepository.sendImage(permanentImageUri, event.userId)
                    }
                }

            }
        }
    }

    private fun promptEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableHotspot() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun isHotspotEnabled(): Boolean {
        return try {
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun savePermanentImage(context: Context, sourceUri: Uri): Uri? {
        try {
            // Create a directory for our images if it doesn't exist
            val imageDir =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "send_images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            // Generate a unique filename
            val imageFileName = "img_${UUID.randomUUID()}.jpg"
            val destFile = File(imageDir, imageFileName)

            // Copy the file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Create a URI from the saved file
            return Uri.fromFile(destFile)
        } catch (e: IOException) {
            Log.e("ProtocolViewModel", "Error saving image: ${e.message}")
            return null
        }
    }

    override fun onCleared() {
        super.onCleared()
        wifiDirectManager.removeOnHotspotStateChangedCallback(::onHotspotStateChanged)
        wifiDirectManager.removeOnLocationStateChangedCallback(::onLocationStateChanged)
        wifiDirectManager.removeOnWifiStateChangedCallback(::onWifiStateChanged)
        wifiDirectManager.removeOnConnectionCallback(::onConnection)
        wifiDirectManager.removeOnDisconnectCallback(::onDisconnect)
        wifiDirectManager.removeOnDeviceNameChangedCallback(::onDeviceNameChanged)
    }
}