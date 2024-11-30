package com.example.zim.wifiP2P

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo

interface WifiP2pListener {
    fun onWifiP2pEnabled()
    fun onWifiP2pDisabled()
    fun onPeersAvailable(peers: Collection<WifiP2pDevice>)
    fun onConnectionInfoAvailable(info: WifiP2pInfo)
    fun onDisconnected()
    fun onThisDeviceChanged(device: WifiP2pDevice?)
}