package com.universalazan.app.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.universalazan.app.data.preferences.TimeFormatSetting
import java.time.temporal.TemporalAccessor
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The user's time-format choice, provided once at the top of the app. */
val LocalTimeFormatSetting = compositionLocalOf { TimeFormatSetting.System }

/** Formats a clock time as "17:05" or "5:05 PM". Pure function, easy to test. */
fun formatClockTime(time: TemporalAccessor, use24Hour: Boolean, locale: Locale): String =
    DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale).format(time)

/** True if times should be shown in 24-hour style, following the setting (or the phone). */
@Composable
fun use24HourClock(): Boolean = when (LocalTimeFormatSetting.current) {
    TimeFormatSetting.TwentyFourHour -> true
    TimeFormatSetting.TwelveHour -> false
    TimeFormatSetting.System -> DateFormat.is24HourFormat(LocalContext.current)
}

/** Formats a clock time using the user's setting and the phone's language. */
@Composable
fun formatClockTime(time: TemporalAccessor): String =
    formatClockTime(time, use24HourClock(), LocalConfiguration.current.locales[0])
