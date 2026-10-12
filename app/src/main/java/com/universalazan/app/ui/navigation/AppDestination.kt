package com.universalazan.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.universalazan.app.R

/** The app's three top-level sections, in the order shown in the navigation bar. */
enum class AppDestination(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    PrayerTimes(R.string.nav_prayer_times, R.drawable.ic_nav_prayer_times),
    Qibla(R.string.nav_qibla, R.drawable.ic_nav_qibla),
    Settings(R.string.nav_settings, R.drawable.ic_nav_settings),
}
