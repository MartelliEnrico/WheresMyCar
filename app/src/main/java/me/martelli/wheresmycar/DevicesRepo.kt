package me.martelli.wheresmycar

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.martelli.wheresmycar.proto.Device
import me.martelli.wheresmycar.proto.Devices
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DevicesRepo(private val context: Context, private val dataStore: DataStore<Devices>) {
    private val data = dataStore.data
        .catch {
            if (it is IOException) {
                emit(Devices.getDefaultInstance())
            } else {
                throw it
            }
        }

    val devices = data.map { it.devicesList }

    suspend fun addDevice(device: Device) {
        dataStore.updateData {
            it.toBuilder().addDevices(device).build()
        }
    }

    suspend fun removeDevice(device: Device) {
        dataStore.updateData {
            val index = it.devicesList.indexOfFirst { d -> d.address == device.address }
            it.toBuilder().removeDevices(index).build()
        }
        removeDynamicShortcut(context, device)
    }

    suspend fun updateDevice(device: Device) {
        dataStore.updateData {
            val index = it.devicesList.indexOfFirst { d -> d.address == device.address }
            it.toBuilder().setDevices(index, device).build()
        }
        if (device.hasLocation) {
            pushDynamicShortcut(context, device)
        }
    }
}

object DevicesSerializer : Serializer<Devices> {
    override val defaultValue: Devices = Devices.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Devices {
        try {
            return Devices.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Devices, output: OutputStream) {
        t.writeTo(output)
    }
}

val Device.hasLocation get() = hasLatitude() && hasLongitude()
