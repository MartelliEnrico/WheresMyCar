package me.martelli.wheresmycar

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val savedDevice = runBlocking { context.savedDevice.first() }

        if (device.address == savedDevice?.address) {
            val workManager = WorkManager.getInstance(context)
            val workRequest = OneTimeWorkRequestBuilder<DeviceLocationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueue(workRequest)
        }
    }
}
