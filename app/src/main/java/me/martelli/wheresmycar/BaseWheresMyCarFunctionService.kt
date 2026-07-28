package me.martelli.wheresmycar

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.martelli.wheresmycar.data.DevicesRepo
import me.martelli.wheresmycar.data.DevicesRepo.Companion.hasLocation

@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "WheresMyCarFunctionService",
    appFunctionXmlFileName = "wheres_my_car_function_service",
)
abstract class BaseWheresMyCarFunctionService : AppFunctionService() {
    @Inject
    internal lateinit var devicesRepo: DevicesRepo

    /** The location of the car. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class Location(
        /** The latitude of the coordinates. */
        val latitude: Double,
        /** The longitude of the coordinates. */
        val longitude: Double,
        /** The Google Maps url of the coordinates. Useful for direct navigation. */
        val url: String
    )

    /**
     * Get the list of available car names.
     * @return The list of all the available car names.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getCarNames(): List<String> = withContext(Dispatchers.IO) {
        val devices = devicesRepo.devices.first()
        return@withContext devices.map { it.name }
    }

    /**
     * Find the last saved parkin location of the car.
     * Required workflow: Call 'getCarNames' first to obtain the list of available car names.
     * @param carName The name of the car to find.
     * @return The coordinates of the last saved parking location.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun findCar(carName: String): Location? = withContext(Dispatchers.IO) {
        val devices = devicesRepo.devices.first()
        val car = devices.firstOrNull { carName.equals(it.name, ignoreCase = true) }
        return@withContext if (car?.hasLocation == true) {
            Location(
                car.latitude,
                car.longitude,
                googleMapsUrl(car)
            )
        } else {
            null
        }
    }
}
