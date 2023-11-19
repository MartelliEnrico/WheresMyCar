package me.martelli.wheresmycar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DeviceDisconnectedReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action !== BluetoothDevice.ACTION_ACL_DISCONNECTED) return

        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val savedAddress = runBlocking { context.dataStore.data.first()[Address] }

        if (device.address == savedAddress) {
            val locationManager = context.getSystemService<LocationManager>()
            locationManager?.getCurrentLocation(LocationManager.FUSED_PROVIDER, null, context.mainExecutor) {
                runBlocking {
                    context.dataStore.edit { preferences ->
                        preferences[Latitude] = it.latitude.toFloat()
                        preferences[Longitude] = it.longitude.toFloat()
                    }
                }
            }
        }
    }
}
