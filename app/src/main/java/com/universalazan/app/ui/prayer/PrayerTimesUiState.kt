package com.universalazan.app.ui.prayer

import androidx.annotation.StringRes
import com.universalazan.app.R
import com.universalazan.app.data.locations.SavedLocation
import java.time.LocalDate

/** Everything the Prayer Times screen can show. */
sealed interface PrayerTimesUiState {
    data object Loading : PrayerTimesUiState

    data class Error(@StringRes val messageRes: Int) : PrayerTimesUiState

    data class Ready(
        /** Null when the user has not chosen a location yet. */
        val location: SavedLocation?,
        val date: LocalDate,
        val prayers: List<PrayerRowUi>,
    ) : PrayerTimesUiState
}

/**
 * One row in the prayer list.
 * [time] is null when no real time has been calculated. The screen then shows "--:--",
 * never an invented time.
 */
data class PrayerRowUi(
    @StringRes val nameRes: Int,
    val isObligatory: Boolean,
    val time: String?,
)

/** The five obligatory prayers plus Sunrise (information only), with no times yet. */
fun notCalculatedPrayerRows(): List<PrayerRowUi> = listOf(
    PrayerRowUi(R.string.prayer_fajr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_sunrise, isObligatory = false, time = null),
    PrayerRowUi(R.string.prayer_dhuhr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_asr, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_maghrib, isObligatory = true, time = null),
    PrayerRowUi(R.string.prayer_isha, isObligatory = true, time = null),
)
