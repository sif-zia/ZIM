package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.ClientRepository
import com.example.zim.api.ServerRepository
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.ProtocolEvent
import com.example.zim.states.ProtocolState
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
    serverRepository: ServerRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProtocolState())

    val activeUsers = activeUserManager.activeUsers

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
                )
            }
        }

        if (!serverRepository.isServerRunning.value)
            serverRepository.startServer()
    }

    @SuppressLint("MissingPermission")
    fun onEvent(event: ProtocolEvent) {
        when (event) {
            is ProtocolEvent.LocationEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLocationEnabled = true) }
                }
            }

            is ProtocolEvent.LocationDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLocationEnabled = false) }
                }
            }

            is ProtocolEvent.WifiEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isWifiEnabled = true) }
                }
            }

            is ProtocolEvent.WifiDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isWifiEnabled = false) }
                }
            }

            is ProtocolEvent.HotspotEnabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isHotspotEnabled = true) }
                }
            }

            is ProtocolEvent.HotspotDisabled -> {
                viewModelScope.launch {
                    _state.update { it.copy(isHotspotEnabled = false) }
                }
            }

            is ProtocolEvent.LaunchEnableLocation -> {
                promptEnableLocation()
            }

            is ProtocolEvent.LaunchEnableWifi -> {
                promptEnableWifi()
            }

            is ProtocolEvent.LaunchEnableHotspot -> {
                promptEnableHotspot()
            }

            is ProtocolEvent.ChangeMyDeviceName -> {
                viewModelScope.launch {
                    userDao.setCurrUserDeviceName(event.newDeviceName)
                }
            }

            is ProtocolEvent.StartClient -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            groupOwnerIp = event.groupOwnerIp ?: "192.168.49.1",
                            amIGroupOwner = false
                        )
                    }
                    clientRepository.handshake(event.groupOwnerIp ?: "192.168.49.1")
                }
            }

            is ProtocolEvent.Disconnect -> {
                activeUserManager.clearAllUsers()
            }

            is ProtocolEvent.SendMessage -> {
                if (event.message.isNotEmpty())
                    viewModelScope.launch {
                        clientRepository.sendMessage(event.message, event.id)
                    }
            }

            is ProtocolEvent.SendImage -> {
                val permanentImageUri= savePermanentImage(application, event.imageUri)
                if(permanentImageUri == null){
                    Toast.makeText(application, "Send Failed.", Toast.LENGTH_SHORT).show()
                }
                else {
                    viewModelScope.launch {
                        clientRepository.sendImage(permanentImageUri, event.userId)
                    }
                }

            }

            is ProtocolEvent.AutoConnect -> {
                viewModelScope.launch {
                    val user = userDao.getUserById(event.userId)

                    _state.value.wifiP2pManager?.requestPeers(_state.value.wifiChannel) { peers ->
                        peers.deviceList.forEach { device ->
                            if (device.deviceName == user.deviceName) {

                                val config = WifiP2pConfig()
                                config.deviceAddress = device.deviceAddress
                                _state.value.wifiP2pManager?.connect(
                                    _state.value.wifiChannel,
                                    config,
                                    object : WifiP2pManager.ActionListener {
                                        override fun onSuccess() {
                                            Toast.makeText(
                                                application,
                                                "Connection Request Sent",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onFailure(p0: Int) {
                                            Toast.makeText(
                                                application,
                                                "Connection Request Failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            }
                        }

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

    fun initWifiManager(wifiP2pManager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    wifiP2pManager = wifiP2pManager,
                    wifiChannel = channel
                )
            }
        }
    }
    fun savePermanentImage(context: Context, sourceUri: Uri): Uri? {
        try {
            // Create a directory for our images if it doesn't exist
            val imageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "send_images")
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
}