package com.universalazan.app.domain.location

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Returns the zone for an IANA identifier such as "Europe/London", or null if it is not valid. */
fun zoneIdOrNull(id: String?): ZoneId? =
    if (id.isNullOrBlank()) null else runCatching { ZoneId.of(id) }.getOrNull()

/**
 * Time zones offered to the user: region-based IANA zones such as "Europe/London".
 * Fixed-offset ("Etc/GMT+3", whose sign is confusingly inverted) and legacy IDs are left out
 * because they do not follow daylight-saving rules.
 */
fun timeZoneChoices(): List<String> =
    ZoneId.getAvailableZoneIds()
        .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
        .sorted()

/** The zone's UTC offset at [instant], e.g. "UTC+01:00". Changes with daylight saving. */
fun utcOffsetLabel(zoneId: ZoneId, instant: Instant = Instant.now()): String {
    val offset = zoneId.rules.getOffset(instant)
    return if (offset == ZoneOffset.UTC) "UTC+00:00" else "UTC${offset.id}"
}
