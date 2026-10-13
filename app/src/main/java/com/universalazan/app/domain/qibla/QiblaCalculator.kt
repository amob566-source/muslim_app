package com.universalazan.app.domain.qibla

import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface QiblaResult {
    /**
     * [bearingDegrees]: initial great-circle bearing to the Kaaba, clockwise from TRUE north,
     * in the range 0 (inclusive) to 360 (exclusive). [distanceKm]: great-circle distance.
     */
    data class Direction(val bearingDegrees: Double, val distanceKm: Double) : QiblaResult

    /** Standing at (or within a few metres of) the Kaaba: there is no single direction. */
    data object AtKaaba : QiblaResult

    /** At the exact opposite point of the Earth: every direction leads to the Kaaba equally. */
    data object Antipode : QiblaResult

    data object InvalidCoordinates : QiblaResult
}

/**
 * Calculates the Qibla direction offline from coordinates alone (no GPS, no internet).
 * Uses spherical great-circle formulas, which agree with ellipsoidal results to within a
 * small fraction of a degree, far finer than any phone compass can show.
 */
object QiblaCalculator {
    /** Approximate coordinates of the Kaaba. */
    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262

    /** Mean Earth radius used for the distance. */
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Closer than this to the Kaaba (or its antipode), no single direction is meaningful. */
    private const val SPECIAL_POINT_KM = 0.05

    fun calculate(latitude: Double, longitude: Double): QiblaResult {
        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) return QiblaResult.InvalidCoordinates

        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        // atan2 below handles any longitude difference, including across the date line.
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - longitude)

        val distanceKm = haversineKm(lat1, lat2, deltaLon)
        if (distanceKm < SPECIAL_POINT_KM) return QiblaResult.AtKaaba
        if (Math.PI * EARTH_RADIUS_KM - distanceKm < SPECIAL_POINT_KM) return QiblaResult.Antipode

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        val bearing = normalizeDegrees(Math.toDegrees(atan2(y, x)))
        return QiblaResult.Direction(bearing, distanceKm)
    }

    private fun haversineKm(lat1: Double, lat2: Double, deltaLon: Double): Double {
        val deltaLat = lat2 - lat1
        val a = sin(deltaLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
    }
}

/** Converts any angle to the range 0 (inclusive) to 360 (exclusive). */
fun normalizeDegrees(degrees: Double): Double {
    val d = degrees % 360.0
    val positive = if (d < 0) d + 360.0 else d
    // Values a hair below 360 can round to exactly 360.0; keep the range half-open.
    return if (positive >= 360.0) 0.0 else positive
}

private val CompassPoints = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

/** 16-point compass name for a bearing, e.g. 119° -> "ESE". */
fun compassPoint(bearingDegrees: Double): String =
    CompassPoints[((normalizeDegrees(bearingDegrees) + 11.25) / 22.5).toInt() % 16]
