package me.martelli.wheresmycar

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore
import me.martelli.wheresmycar.data.ConfigsRepo
import me.martelli.wheresmycar.data.ConfigsSerializer
import me.martelli.wheresmycar.data.DevicesRepo
import me.martelli.wheresmycar.data.DevicesSerializer

class MyApplication : Application() {
    lateinit var configs: ConfigsRepo
    lateinit var devices: DevicesRepo

    override fun onCreate() {
        super.onCreate()
        configs = ConfigsRepo(configsDataStore)
        devices = DevicesRepo(this, devicesDataStore)
    }

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
