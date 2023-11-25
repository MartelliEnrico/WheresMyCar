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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.edit
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
        val notificationChannel = NotificationChannel(NotificationChannelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        notificationManager!!.createNotificationChannel(notificationChannel)

        val title = applicationContext.getString(R.string.notification_title)
        val cancel = applicationContext.getString(R.string.cancel)
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, NotificationId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(title)
            .setSmallIcon(R.drawable.directions_car)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        setForeground(ForegroundInfo(NotificationIdNumber, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION))

        val location = getLocation()

        applicationContext.dataStore.edit { preferences ->
            preferences[Latitude] = location.latitude.toFloat()
            preferences[Longitude] = location.longitude.toFloat()
            preferences[Time] = location.time
        }

        val shortcut = ShortcutInfoCompat.Builder(applicationContext, ShortcutId)
            .setShortLabel(applicationContext.getString(R.string.shortcut_short_description))
            .setLongLabel(applicationContext.getString(R.string.shortcut_long_description))
            .setIcon(IconCompat.createWithResource(applicationContext, R.drawable.directions_car))
            .setIntent(locationIntent(location.latitude, location.longitude))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(applicationContext, shortcut)

        return Result.success()
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private suspend fun getLocation() = suspendCancellableCoroutine { cont ->
        val locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, IntervalMillis)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(IntervalMillis)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    locationClient.removeLocationUpdates(this)

                    cont.resume(it)
                }
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnFailureListener { cont.cancel(it) }

        cont.invokeOnCancellation { locationClient.removeLocationUpdates(locationCallback) }
    }

    companion object {
        const val NotificationChannelId = "location"
        const val NotificationId = "location"
        const val NotificationIdNumber = 1
        const val IntervalMillis = 100L
    }
}