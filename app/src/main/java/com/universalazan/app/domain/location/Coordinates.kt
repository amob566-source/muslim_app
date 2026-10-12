package com.universalazan.app.domain.location

import java.util.Locale
import kotlin.math.abs

enum class CoordinateAxis(val min: Double, val max: Double) {
    Latitude(-90.0, 90.0),
    Longitude(-180.0, 180.0),
}

sealed interface CoordinateParseResult {
    data class Valid(val value: Double) : CoordinateParseResult
    data object Empty : CoordinateParseResult
    data object NotANumber : CoordinateParseResult
    data object OutOfRange : CoordinateParseResult
}

// Plain decimal numbers only, e.g. "51.5074", "-0.1278", "+12", ".5". No exponents or suffixes.
private val DecimalPattern = Regex("""^[+-]?(\d+(\.\d*)?|\.\d+)$""")

/**
 * Parses a latitude or longitude typed by the user.
 * A single comma is accepted as the decimal separator ("51,5074"), as used in many countries.
 */
fun parseCoordinate(text: String, axis: CoordinateAxis): CoordinateParseResult {
    // Some keyboards produce a Unicode minus sign instead of a hyphen.
    val trimmed = text.trim().replace('−', '-')
    if (trimmed.isEmpty()) return CoordinateParseResult.Empty
    val normalized = if ('.' !in trimmed && trimmed.count { it == ',' } == 1) {
        trimmed.replace(',', '.')
    } else {
        trimmed
    }
    if (!DecimalPattern.matches(normalized)) return CoordinateParseResult.NotANumber
    val value = normalized.toDoubleOrNull() ?: return CoordinateParseResult.NotANumber
    if (value < axis.min || value > axis.max) return CoordinateParseResult.OutOfRange
    return CoordinateParseResult.Valid(value)
}

fun isValidLatitude(value: Double): Boolean = !value.isNaN() && value in -90.0..90.0

fun isValidLongitude(value: Double): Boolean = !value.isNaN() && value in -180.0..180.0

/** Formats coordinates for display, e.g. "51.5074° N, 0.1278° W". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude < 0) "S" else "N"
    val ew = if (longitude < 0) "W" else "E"
    return String.format(Locale.ROOT, "%.4f° %s, %.4f° %s", abs(latitude), ns, abs(longitude), ew)
}
