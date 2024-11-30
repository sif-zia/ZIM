package com.example.zim.wifiP2Pimport


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.widget.Toast
import com.example.zim.wifiP2P.LocationListener

class LocationBroadcastReceiver(private val locationListener: LocationListener): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            locationListener.onLocationChange()
        }
    }
}