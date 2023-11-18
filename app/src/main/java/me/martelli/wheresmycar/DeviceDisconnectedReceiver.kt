package me.martelli.wheresmycar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.content.getSystemService

class DeviceDisconnectedReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action !== BluetoothDevice.ACTION_ACL_DISCONNECTED) return

        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return

        val sharedPref = context.getSharedPreferences(SharedPreference, Context.MODE_PRIVATE)
        val savedAddress = sharedPref?.getString(Address, null)

        if (device.address == savedAddress) {
            val locationManager = context.getSystemService<LocationManager>()
            locationManager?.getCurrentLocation(LocationManager.FUSED_PROVIDER, null, context.mainExecutor) {
                with (sharedPref.edit()) {
                    putFloat(Latitude, it.latitude.toFloat())
                    putFloat(Longitude, it.longitude.toFloat())
                    apply()
                }
            }
        }
    }
}
