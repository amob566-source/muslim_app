package com.universalazan.app.data.locations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CitySearchTest {
    private fun city(name: String, cc: String, population: Long, vararg aliases: String) =
        City(name, cc, "", 0.0, 0.0, "UTC", population, aliases.toList())

    private val search = CitySearch(
        listOf(
            city("London", "GB", 8_961_989),
            city("London", "CA", 422_324),
            city("Londonderry", "GB", 83_652),
            city("New London", "US", 27_000),
            city("Makkah", "SA", 1_578_722, "Mecca", "Makkah al Mukarramah"),
            city("Madinah", "SA", 1_300_000, "Medina", "Al Madinah"),
            city("Medina", "US", 26_000),
            city("São Paulo", "BR", 12_000_000, "Sao Paulo"),
            city("Tromsø", "NO", 41_915, "Tromso"),
        ),
    )

    private fun names(query: String) = search.search(query).map { "${it.name}/${it.countryCode}" }

    @Test
    fun exactMatchesComeFirstThenByPopulation() {
        assertEquals(
            listOf("London/GB", "London/CA", "Londonderry/GB", "New London/US"),
            names("london"),
        )
    }

    @Test
    fun commonEnglishNamesFindHolyCities() {
        assertEquals("Makkah/SA", names("Mecca").first())
        // The large city wins over the small exact-name match.
        assertEquals(listOf("Madinah/SA", "Medina/US"), names("Medina"))
    }

    @Test
    fun ignoresAccentsAndCase() {
        assertEquals(listOf("São Paulo/BR"), names("SAO PAULO"))
        assertEquals(listOf("São Paulo/BR"), names("são"))
        assertEquals(listOf("Tromsø/NO"), names("tromso"))
    }

    @Test
    fun matchesLaterWordsInName() {
        assertTrue("New London/US" in names("lon"))
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertTrue(search.search("   ").isEmpty())
    }

    @Test
    fun respectsLimit() {
        assertEquals(2, search.search("lo", limit = 2).size)
    }

    @Test
    fun parsesDatasetLines() {
        val line = "Makkah\t\tSA\tMecca Region\t21.4266\t39.8256\tAsia/Riyadh\t1578722\tMecca|Makka"
        val parsed = parseCityLine(line)!!
        assertEquals("Makkah", parsed.name)
        assertEquals(21.4266, parsed.latitude, 0.0)
        assertEquals("Asia/Riyadh", parsed.timeZoneId)
        assertEquals(listOf("Mecca", "Makka"), parsed.searchNames)
        assertNull(parseCityLine("# header comment"))
        assertNull(parseCityLine("Broken\t\tXX\t\t95.0\t0.0\tUTC\t1"))
        assertNull(parseCityLine("too\tfew\tcolumns"))
    }
}
