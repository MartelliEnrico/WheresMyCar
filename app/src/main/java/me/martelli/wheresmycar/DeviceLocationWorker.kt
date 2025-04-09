package me.martelli.wheresmycar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DeviceLocationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        val channelName = applicationContext.getString(R.string.channel_name)
        val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        notificationManager!!.createNotificationChannel(notificationChannel)

        val title = applicationContext.getString(R.string.notification_title)
        val cancel = applicationContext.getString(R.string.cancel)
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(title)
            .setSmallIcon(R.drawable.directions_car)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        setForeground(ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION))

        val location = getLocation()
        val address = inputData.getString(ADDRESS)!!

        val devices = applicationContext.devicesDataStore.updateData { devices ->
            val index = devices.devicesList.indexOfFirst { it.address == address }
            val device = devices.devicesList[index]
            devices.toBuilder().setDevices(index,
                device.toBuilder()
                    .setLatitude(location.latitude)
                    .setLongitude(location.longitude)
                    .setTime(location.time)
            ).build()
        }

        val device = devices.devicesList.first { it.address == address }
        pushDynamicShortcut(applicationContext, device)

        return Result.success()
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private suspend fun getLocation() = suspendCancellableCoroutine { cont ->
        val locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MILLIS)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(INTERVAL_MILLIS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    if (cont.isActive) {
                        cont.resume(it)
                    }
                }
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnFailureListener { cont.cancel(it) }

        cont.invokeOnCancellation { locationClient.removeLocationUpdates(locationCallback) }
    }

    companion object {
        const val ADDRESS = "address"
        const val CHANNEL_ID = "location"
        const val NOTIFICATION_ID = 1
        const val INTERVAL_MILLIS = 100L
    }
}
