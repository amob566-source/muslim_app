package com.universalazan.app.data.locations

import com.universalazan.app.domain.location.zoneIdOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled city file (app/src/main/assets/cities.tsv) itself. */
class CityDatasetTest {
    private val lines = File("src/main/assets/cities.tsv").readLines(Charsets.UTF_8)
    private val cities = lines.mapNotNull(::parseCityLine)

    @Test
    fun startsWithLicenceAttribution() {
        assertTrue(lines.first().startsWith("# City data from GeoNames"))
        assertTrue(lines.first().contains("CC BY 4.0"))
    }

    @Test
    fun everyDataLineParses() {
        val dataLines = lines.count { it.isNotBlank() && !it.startsWith("#") }
        assertEquals(dataLines, cities.size)
        assertTrue("expected over 30,000 cities, got ${cities.size}", cities.size > 30_000)
    }

    @Test
    fun everyCityHasAValidTimeZone() {
        val bad = cities.filter { zoneIdOrNull(it.timeZoneId) == null }
        assertTrue("cities with unknown time zone: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun includesCitiesWorldwide() {
        val search = CitySearch(cities)
        fun first(query: String) = search.search(query).firstOrNull()
        val checks = mapOf(
            "Mecca" to ("SA" to "Asia/Riyadh"),
            "Medina" to ("SA" to "Asia/Riyadh"),
            "London" to ("GB" to "Europe/London"),
            "Jakarta" to ("ID" to "Asia/Jakarta"),
            "Karachi" to ("PK" to "Asia/Karachi"),
            "Cairo" to ("EG" to "Africa/Cairo"),
            "New York City" to ("US" to "America/New_York"),
            "Auckland" to ("NZ" to "Pacific/Auckland"),
            "Tromso" to ("NO" to "Europe/Oslo"),
        )
        checks.forEach { (query, expected) ->
            val city = first(query)
            assertNotNull("no result for $query", city)
            assertEquals("country for $query", expected.first, city!!.countryCode)
            assertEquals("time zone for $query", expected.second, city.timeZoneId)
        }
    }

    @Test
    fun makkahCoordinatesAreNearTheKaaba() {
        val makkah = CitySearch(cities).search("Makkah").first()
        assertEquals(21.42, makkah.latitude, 0.05)
        assertEquals(39.83, makkah.longitude, 0.05)
    }
}
