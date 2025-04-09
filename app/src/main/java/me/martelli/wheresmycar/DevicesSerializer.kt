package me.martelli.wheresmycar

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.catch
import me.martelli.wheresmycar.proto.Device
import me.martelli.wheresmycar.proto.Devices
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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

internal fun devicesMigrations(context: Context) = listOf(
    SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = "saved_device"
    ) { prefs, devices: Devices ->
        devices.toBuilder().addDevices(
            Device.newBuilder()
                .setAddress(prefs.getString("address", ""))
                .setOriginalName(prefs.getString("name", ""))
                .setName(prefs.getString("display_name", prefs.getString("name", "")))
                .setLatitude(prefs.getFloat("latitude", 0f).toDouble())
                .setLongitude(prefs.getFloat("longitude", 0f).toDouble())
                .setTime(prefs.getLong("time", 0))
        ).build()
    }
)

val Context.devicesDataStore by dataStore(
    fileName = "devices.pb",
    serializer = DevicesSerializer,
    produceMigrations = ::devicesMigrations,
)

val Context.devices
    get() = devicesDataStore.data
        .catch {
            if (it is IOException) {
                emit(Devices.getDefaultInstance())
            } else {
                throw it
            }
        }

val Device.hasLocation get() = hasLatitude() && hasLongitude()
