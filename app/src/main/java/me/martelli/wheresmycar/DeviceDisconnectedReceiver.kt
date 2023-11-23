package me.martelli.wheresmycar

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.datastore.preferences.core.edit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
            val task = locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            task.addOnSuccessListener { location ->
                location?.let {
                    runBlocking {
                        context.dataStore.edit { preferences ->
                            preferences[Latitude] = it.latitude.toFloat()
                            preferences[Longitude] = it.longitude.toFloat()
                        }
                    }

                    pushDynamicShortcut(context, it.latitude, it.longitude)
                }
            }
        }
    }
}
