package me.martelli.wheresmycar

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.catch
import me.martelli.wheresmycar.proto.Configs
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ConfigsSerializer : Serializer<Configs> {
    override val defaultValue: Configs = Configs.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Configs {
        try {
            return Configs.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Configs, output: OutputStream) {
        t.writeTo(output)
    }
}

internal fun configsMigrations(context: Context) = listOf(
    SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = "configurations"
    ) { prefs, configs: Configs ->
        configs.toBuilder().setOnboardingCompleted(prefs.getBoolean("onboarding_completed", false)).build()
    }
)

val Context.configsDataStore by dataStore(
    fileName = "configs.pb",
    serializer = ConfigsSerializer,
    produceMigrations = ::configsMigrations,
)

val Context.configs
    get() = configsDataStore.data
        .catch {
            if (it is IOException) {
                emit(Configs.getDefaultInstance())
            } else {
                throw it
            }
        }
