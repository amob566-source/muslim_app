package com.universalazan.app.ui.prayer

import androidx.annotation.StringRes
import com.universalazan.app.R
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.DailyPrayerTimes
import java.time.LocalDate
import java.time.ZonedDateTime

/** Everything the Prayer Times screen can show. */
sealed interface PrayerTimesUiState {
    data object Loading : PrayerTimesUiState

    data class Error(@StringRes val messageRes: Int) : PrayerTimesUiState

    data class Ready(
        /** Null when the user has not chosen a location yet. */
        val location: SavedLocation?,
        /** Today's date at the selected location. */
        val date: LocalDate,
        val prayers: List<PrayerRowUi>,
        val settings: CalculationSettings? = null,
        /** Why times are missing, if they are. */
        val problem: PrayerProblem? = null,
    ) : PrayerTimesUiState
}

enum class PrayerProblem {
    NoLocation,
    InvalidTimeZone,
    InvalidCoordinates,
    SunDoesNotRiseOrSet,
}

/**
 * One row in the prayer list.
 * [time] is null when no time could be calculated; the screen then shows "--:--",
 * never an invented time.
 */
data class PrayerRowUi(
    @StringRes val nameRes: Int,
    val isObligatory: Boolean,
    val time: ZonedDateTime?,
    val isNext: Boolean = false,
)

/** The five obligatory prayers plus Sunrise (information only), with no times. */
fun notCalculatedPrayerRows(): List<PrayerRowUi> = listOf(
    PrayerRowUi(R.string.prayer_fajr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_sunrise, isObligatory = false, time = null),
    PrayerRowUi(R.string.prayer_dhuhr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_asr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_maghrib, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_isha, isObligatory = true, time = null),
)

/**
 * Rows for calculated times. The next obligatory prayer after [now] is marked;
 * Sunrise is never marked because it is not a prayer.
 */
fun calculatedPrayerRows(times: DailyPrayerTimes, now: ZonedDateTime?): List<PrayerRowUi> {
    val rows = listOf(
        PrayerRowUi(R.string.prayer_fajr, isObligatory = true, time = times.fajr),
        PrayerRowUi(R.string.prayer_sunrise, isObligatory = false, time = times.sunrise),
        PrayerRowUi(R.string.prayer_dhuhr, isObligatory = true, time = times.dhuhr),
        PrayerRowUi(R.string.prayer_asr, isObligatory = true, time = times.asr),
        PrayerRowUi(R.string.prayer_maghrib, isObligatory = true, time = times.maghrib),
        PrayerRowUi(R.string.prayer_isha, isObligatory = true, time = times.isha),
    )
    if (now == null) return rows
    val nextIndex = rows.indexOfFirst { it.isObligatory && it.time != null && it.time.isAfter(now) }
    return rows.mapIndexed { index, row -> if (index == nextIndex) row.copy(isNext = true) else row }
}
