package me.martelli.wheresmycar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.appfunctions.service.AppFunctionConfiguration
import androidx.datastore.dataStore
import me.martelli.wheresmycar.data.ConfigsRepo
import me.martelli.wheresmycar.data.ConfigsSerializer
import me.martelli.wheresmycar.data.DevicesRepo
import me.martelli.wheresmycar.data.DevicesSerializer

class MyApplication : Application(), AppFunctionConfiguration.Provider {
    lateinit var configs: ConfigsRepo
    lateinit var devices: DevicesRepo
    lateinit var locationFunctions: LocationFunctions

    override fun onCreate() {
        super.onCreate()
        configs = ConfigsRepo(configsDataStore)
        devices = DevicesRepo(this, devicesDataStore)
        locationFunctions = LocationFunctions(devices)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            DeviceLocationWorker.CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() =
            AppFunctionConfiguration.Builder()
                .addEnclosingClassFactory(LocationFunctions::class.java) { locationFunctions }
                .build()

    companion object {
        private val Context.configsDataStore by dataStore(
            fileName = "configs.pb",
            serializer = ConfigsSerializer,
        )

        private val Context.devicesDataStore by dataStore(
            fileName = "devices.pb",
            serializer = DevicesSerializer,
        )
    }
}
