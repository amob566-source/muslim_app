package com.universalazan.app.alarms

import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.DailyPrayerTimes
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.domain.prayer.PrayerCalculationResult
import com.universalazan.app.domain.prayer.PrayerTimeCalculator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** One Azan to schedule. */
data class PlannedAlarm(
    val prayer: Prayer,
    /** The prayer-time date (at the location) this alarm belongs to. */
    val date: LocalDate,
    val time: Instant,
    val sound: AzanSound,
) {
    /** Identifies this exact prayer occurrence, e.g. "Dhuhr@2026-10-13". */
    val id: String get() = "${prayer.name}@$date"
}

fun DailyPrayerTimes.timeOf(prayer: Prayer): ZonedDateTime = when (prayer) {
    Prayer.Fajr -> fajr
    Prayer.Dhuhr -> dhuhr
    Prayer.Asr -> asr
    Prayer.Maghrib -> maghrib
    Prayer.Isha -> isha
}

/**
 * Works out the next Azan for each prayer. Pure function: no Android, easy to test.
 *
 * Strategy: one alarm per prayer, always the next future occurrence. When it fires, planning
 * again simply moves that prayer to its next day. Silent prayers get no alarm.
 */
object PrayerAlarmPlanner {
    /** How many days ahead to look if a prayer cannot be calculated (e.g. polar summer). */
    private const val LOOKAHEAD_DAYS = 3L

    /** An alarm that fires later than this after its time is treated as missed and not played. */
    val MISSED_TOLERANCE: Duration = Duration.ofMinutes(5)

    fun plan(
        location: SavedLocation?,
        calculation: CalculationSettings,
        sounds: AzanSoundSettings,
        now: Instant,
    ): List<PlannedAlarm> {
        if (location == null) return emptyList()
        val zone = location.zoneId ?: return emptyList()
        val today = now.atZone(zone).toLocalDate()
        // Start from yesterday: in some places yesterday's Isha falls after midnight.
        val dates = (-1L..LOOKAHEAD_DAYS).map { today.plusDays(it) }
        val daily = dates.mapNotNull { date ->
            (PrayerTimeCalculator.calculate(location.latitude, location.longitude, date, zone, calculation)
                as? PrayerCalculationResult.Success)?.times
        }
        return Prayer.entries.mapNotNull { prayer ->
            val sound = sounds.soundFor(prayer)
            if (sound == AzanSound.Silent) return@mapNotNull null
            daily.asSequence()
                .map { PlannedAlarm(prayer, it.date, it.timeOf(prayer).toInstant(), sound) }
                .firstOrNull { it.time.isAfter(now) }
        }.sortedBy { it.time }
    }

    /** True if an alarm for [scheduledTime] arriving at [now] is too late to play. */
    fun isMissed(scheduledTime: Instant, now: Instant): Boolean =
        Duration.between(scheduledTime, now) > MISSED_TOLERANCE
}
