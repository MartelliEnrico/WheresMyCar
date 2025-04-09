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

class DeviceDisconnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return

        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val devices = runBlocking { context.devices.first() }.devicesList

        if (devices.any { it.address == device.address }) {
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
