package me.martelli.wheresmycar.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.martelli.wheresmycar.proto.Configs
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConfigsRepo(context: Context) {
    private val dataStore = context.configsDataStore
    private val data = dataStore.data
        .catch {
            if (it is IOException) {
                emit(Configs.getDefaultInstance())
            } else {
                throw it
            }
        }

    val onboardingCompleted = data.map { it.onboardingCompleted }

    suspend fun completeOnboarding() {
        dataStore.updateData {
            it.toBuilder().setOnboardingCompleted(true).build()
        }
    }

    companion object {
        private val Context.configsDataStore by dataStore(
            fileName = "configs.pb",
            serializer = ConfigsSerializer,
        )

        private object ConfigsSerializer : Serializer<Configs> {
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
    }
}
