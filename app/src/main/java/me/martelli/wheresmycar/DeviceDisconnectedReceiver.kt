package me.martelli.wheresmycar

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.datastore.preferences.core.edit
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DeviceDisconnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return
        if (checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val savedAddress = runBlocking { context.dataStore.data.first()[Address] }

        if (device.address == savedAddress) {
            val locationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(true)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        locationClient.removeLocationUpdates(this)

                        runBlocking {
                            context.dataStore.edit { preferences ->
                                preferences[Latitude] = it.latitude.toFloat()
                                preferences[Longitude] = it.longitude.toFloat()
                                preferences[Time] = it.time
                            }
                        }

                        pushDynamicShortcut(context, it.latitude, it.longitude)
                    }
                }
            }
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }
}
