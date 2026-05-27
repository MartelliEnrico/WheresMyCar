package me.martelli.wheresmycar

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class DeviceConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val devicesRepo = (context.applicationContext as MyApplication).devices
        val devices = runBlocking { devicesRepo.devices.first() }

        val savedDevice = devices.firstOrNull { it.address == device.address } ?: return
        val currentTime = System.currentTimeMillis()

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val updatedDevice = savedDevice.toBuilder()
                    .setLastConnectedTime(currentTime)
                    .build()
                runBlocking { devicesRepo.updateDevice(updatedDevice) }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val lastConnectedTime = savedDevice.lastConnectedTime
                if (lastConnectedTime > 0) {
                    if ((currentTime - lastConnectedTime).milliseconds < 1.minutes) {
                        return
                    }
                }

                val workManager = WorkManager.getInstance(context)

                val inputData = Data.Builder().apply {
                    putString(DeviceLocationWorker.ADDRESS, device.address)
                }.build()

                val workRequest = OneTimeWorkRequestBuilder<DeviceLocationWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(inputData)
                    .build()

                workManager.enqueue(workRequest)
            }
        }
    }
}
