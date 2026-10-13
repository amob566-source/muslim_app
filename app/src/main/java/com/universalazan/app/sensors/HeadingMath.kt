package com.universalazan.app.sensors

import com.universalazan.app.domain.qibla.normalizeDegrees
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Pure maths for the compass, kept free of Android classes so it can be unit tested.
 *
 * Rotation matrices are Android's 3x3 row-major device-to-world matrices (as produced by
 * SensorManager.getRotationMatrix / getRotationMatrixFromVector): world X = east,
 * world Y = north, world Z = up.
 */
object HeadingMath {

    /**
     * Compass heading of the phone in degrees clockwise from the reference north of [r]
     * (magnetic north for raw sensor data). Returns null if the direction is undefined.
     *
     * The heading combines two directions so it works however the phone is held:
     * - the top edge of the *screen* (best when the phone lies flat), and
     * - the back of the phone, where the camera points (best when it is held upright).
     * For a normal grip both point the same way, so adding them gives a smooth result.
     *
     * [displayRotation] is Surface.ROTATION_0..ROTATION_270 as 0..3; in landscape the top of the
     * screen is a different edge of the device.
     */
    fun heading(r: FloatArray, displayRotation: Int): Double? {
        require(r.size >= 9) { "Rotation matrix must have 9 values" }
        // Top edge of the screen, in device coordinates.
        val (upX, upY) = when (displayRotation and 3) {
            0 -> 0f to 1f
            1 -> 1f to 0f
            2 -> 0f to -1f
            else -> -1f to 0f
        }
        val eastScreen = r[0] * upX + r[1] * upY
        val northScreen = r[3] * upX + r[4] * upY
        // Back of the phone (device -Z).
        val eastBack = -r[2]
        val northBack = -r[5]

        val east = (eastScreen + eastBack).toDouble()
        val north = (northScreen + northBack).toDouble()
        if (hypot(east, north) < 1e-3) return null
        return normalizeDegrees(Math.toDegrees(atan2(east, north)))
    }

    /**
     * How far to turn, in degrees from -180 to 180, to face [targetBearing] from [heading].
     * Positive means turn right (clockwise), negative means turn left.
     */
    fun signedTurn(heading: Double, targetBearing: Double): Double {
        val delta = normalizeDegrees(targetBearing - heading)
        return if (delta > 180.0) delta - 360.0 else delta
    }

    fun isFacing(heading: Double, targetBearing: Double, toleranceDegrees: Double = FACING_TOLERANCE_DEGREES): Boolean =
        abs(signedTurn(heading, targetBearing)) <= toleranceDegrees

    /**
     * Earth's magnetic field is about 25-65 microtesla at the surface. Readings well outside
     * that range mean something nearby (magnet, metal, electronics) is disturbing the sensor.
     */
    fun isMagneticInterference(fieldStrengthMicroTesla: Float): Boolean =
        fieldStrengthMicroTesla < MIN_EARTH_FIELD_UT || fieldStrengthMicroTesla > MAX_EARTH_FIELD_UT

    const val FACING_TOLERANCE_DEGREES = 5.0
    const val MIN_EARTH_FIELD_UT = 20f
    const val MAX_EARTH_FIELD_UT = 70f
}

/**
 * Smooths a stream of angles. Averages the angles' sine and cosine, so 359° and 1° average
 * to 0°, not 180°. [alpha] is the weight of each new reading (0 to 1).
 */
class AngleSmoother(private val alpha: Double = 0.2) {
    private var sinAvg = 0.0
    private var cosAvg = 0.0
    private var started = false

    fun add(degrees: Double): Double {
        val rad = Math.toRadians(degrees)
        if (!started) {
            sinAvg = sin(rad)
            cosAvg = cos(rad)
            started = true
        } else {
            sinAvg += alpha * (sin(rad) - sinAvg)
            cosAvg += alpha * (cos(rad) - cosAvg)
        }
        return normalizeDegrees(Math.toDegrees(atan2(sinAvg, cosAvg)))
    }

    fun reset() {
        started = false
    }
}
