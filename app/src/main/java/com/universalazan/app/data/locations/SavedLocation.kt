package com.universalazan.app.data.locations

import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import com.universalazan.app.domain.location.zoneIdOrNull
import java.time.ZoneId

/** How the user chose their location. */
enum class LocationSource { City, Manual, Device }

/** The location prayer times and the Qibla are calculated for. Stored on the device only. */
data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** IANA time zone identifier, e.g. "Europe/London". */
    val timeZoneId: String,
    val source: LocationSource,
) {
    init {
        require(name.isNotBlank()) { "Location name must not be blank" }
        require(isValidLatitude(latitude)) { "Invalid latitude: $latitude" }
        require(isValidLongitude(longitude)) { "Invalid longitude: $longitude" }
    }

    /** Null if the stored time zone is not recognised; the user must then choose one. */
    val zoneId: ZoneId? get() = zoneIdOrNull(timeZoneId)
}
