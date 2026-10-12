package com.universalazan.app.data.locations

import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import java.text.Normalizer
import java.util.Locale

/** One city from the bundled GeoNames list (assets/cities.tsv). */
data class City(
    val name: String,
    val countryCode: String,
    /** State, province or region; may be empty. */
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val population: Long,
    /** Other spellings used for searching, e.g. "Mecca" for Makkah. */
    val searchNames: List<String> = emptyList(),
) {
    /** Country name in the phone's language, e.g. "United Kingdom" for "GB". */
    fun countryName(locale: Locale = Locale.getDefault()): String =
        runCatching { Locale.Builder().setRegion(countryCode).build().getDisplayCountry(locale) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: countryCode

    fun toSavedLocation(): SavedLocation =
        SavedLocation(name, latitude, longitude, timeZoneId, LocationSource.City)
}

/**
 * Parses one line of cities.tsv. Returns null for the header comment and malformed lines.
 * Columns: name, ascii name, country, region, latitude, longitude, time zone, population, aliases.
 */
fun parseCityLine(line: String): City? {
    if (line.isBlank() || line.startsWith("#")) return null
    val p = line.split('\t')
    if (p.size < 8) return null
    val latitude = p[4].toDoubleOrNull() ?: return null
    val longitude = p[5].toDoubleOrNull() ?: return null
    if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) return null
    if (p[0].isBlank() || p[6].isBlank()) return null
    val searchNames = buildList {
        if (p[1].isNotBlank()) add(p[1])
        if (p.size > 8 && p[8].isNotBlank()) addAll(p[8].split('|'))
    }
    return City(
        name = p[0],
        countryCode = p[2],
        region = p[3],
        latitude = latitude,
        longitude = longitude,
        timeZoneId = p[6],
        population = p[7].toLongOrNull() ?: 0L,
        searchNames = searchNames,
    )
}

private val CombiningMarks = Regex("\\p{Mn}+")
private val Spaces = Regex("\\s+")

/** Lower-case, accent-free form used for matching: "São Paulo" -> "sao paulo". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace('-', ' ')
        .replace("'", "")
        .replace(Spaces, " ")
        .trim()

/** Offline city search. Exact matches first, then names starting with the query, then words. */
class CitySearch(cities: List<City>) {
    private class Entry(val city: City, val keys: List<String>)

    private val entries = cities.map { city ->
        Entry(city, (listOf(city.name) + city.searchNames).map(::normalizeForSearch).distinct())
    }

    val size: Int get() = entries.size

    fun search(query: String, limit: Int = 20): List<City> {
        val q = normalizeForSearch(query)
        if (q.isEmpty()) return emptyList()
        val wordQuery = " $q"
        return entries.asSequence()
            .mapNotNull { entry ->
                val rank = when {
                    entry.keys.any { it == q } -> 0
                    entry.keys.any { it.startsWith(q) } -> 1
                    entry.keys.any { it.contains(wordQuery) } -> 2
                    else -> return@mapNotNull null
                }
                rank to entry.city
            }
            .sortedWith(compareBy<Pair<Int, City>> { it.first }.thenByDescending { it.second.population })
            .take(limit)
            .map { it.second }
            .toList()
    }
}
