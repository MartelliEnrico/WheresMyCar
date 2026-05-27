package me.martelli.wheresmycar

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import kotlinx.coroutines.flow.first
import me.martelli.wheresmycar.data.DevicesRepo
import me.martelli.wheresmycar.data.hasLocation

class LocationFunctions(
    private val devicesRepo: DevicesRepo
) {
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
    suspend fun getCarNames(
        appFunctionContext: AppFunctionContext
    ): List<String> {
        val devices = devicesRepo.devices.first()
        return devices.map { it.name }
    }

    /**
     * Find the last saved parkin location of the car.
     * Required workflow: Call 'getCarNames' first to obtain the list of available car names.
     * @param carName The name of the car to find.
     * @return The coordinates of the last saved parking location.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun findCar(
        appFunctionContext: AppFunctionContext,
        carName: String
    ): Location? {
        val devices = devicesRepo.devices.first()
        val car = devices.firstOrNull { carName.equals(it.name, ignoreCase = true) }
        if (car?.hasLocation == true) {
            return Location(
                car.latitude,
                car.longitude,
                googleMapsUrl(car)
            )
        }
        return null
    }
}
