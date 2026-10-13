package com.universalazan.app.ui.prayer

import androidx.annotation.StringRes
import com.universalazan.app.R
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerCalculationMethod

/** Display names and descriptions for the calculation settings. */
@get:StringRes
val PrayerCalculationMethod.labelRes: Int
    get() = when (this) {
        PrayerCalculationMethod.MuslimWorldLeague -> R.string.method_mwl
        PrayerCalculationMethod.Egyptian -> R.string.method_egyptian
        PrayerCalculationMethod.Karachi -> R.string.method_karachi
        PrayerCalculationMethod.UmmAlQura -> R.string.method_umm_al_qura
        PrayerCalculationMethod.Dubai -> R.string.method_dubai
        PrayerCalculationMethod.MoonsightingCommittee -> R.string.method_moonsighting
        PrayerCalculationMethod.NorthAmerica -> R.string.method_north_america
        PrayerCalculationMethod.Kuwait -> R.string.method_kuwait
        PrayerCalculationMethod.Qatar -> R.string.method_qatar
        PrayerCalculationMethod.Singapore -> R.string.method_singapore
        PrayerCalculationMethod.Turkey -> R.string.method_turkey
    }

@get:StringRes
val PrayerCalculationMethod.descriptionRes: Int
    get() = when (this) {
        PrayerCalculationMethod.MuslimWorldLeague -> R.string.method_mwl_desc
        PrayerCalculationMethod.Egyptian -> R.string.method_egyptian_desc
        PrayerCalculationMethod.Karachi -> R.string.method_karachi_desc
        PrayerCalculationMethod.UmmAlQura -> R.string.method_umm_al_qura_desc
        PrayerCalculationMethod.Dubai -> R.string.method_dubai_desc
        PrayerCalculationMethod.MoonsightingCommittee -> R.string.method_moonsighting_desc
        PrayerCalculationMethod.NorthAmerica -> R.string.method_north_america_desc
        PrayerCalculationMethod.Kuwait -> R.string.method_kuwait_desc
        PrayerCalculationMethod.Qatar -> R.string.method_qatar_desc
        PrayerCalculationMethod.Singapore -> R.string.method_singapore_desc
        PrayerCalculationMethod.Turkey -> R.string.method_turkey_desc
    }

@get:StringRes
val AsrMadhab.shortLabelRes: Int
    get() = when (this) {
        AsrMadhab.Standard -> R.string.asr_standard
        AsrMadhab.Hanafi -> R.string.asr_hanafi
    }

@get:StringRes
val AsrMadhab.labelRes: Int
    get() = when (this) {
        AsrMadhab.Standard -> R.string.asr_standard_long
        AsrMadhab.Hanafi -> R.string.asr_hanafi
    }

@get:StringRes
val AsrMadhab.descriptionRes: Int
    get() = when (this) {
        AsrMadhab.Standard -> R.string.asr_standard_desc
        AsrMadhab.Hanafi -> R.string.asr_hanafi_desc
    }

@get:StringRes
val HighLatitudeOption.labelRes: Int
    get() = when (this) {
        HighLatitudeOption.Automatic -> R.string.high_lat_automatic
        HighLatitudeOption.MiddleOfTheNight -> R.string.high_lat_middle
        HighLatitudeOption.SeventhOfTheNight -> R.string.high_lat_seventh
        HighLatitudeOption.TwilightAngle -> R.string.high_lat_twilight
    }

@get:StringRes
val HighLatitudeOption.descriptionRes: Int
    get() = when (this) {
        HighLatitudeOption.Automatic -> R.string.high_lat_automatic_desc
        HighLatitudeOption.MiddleOfTheNight -> R.string.high_lat_middle_desc
        HighLatitudeOption.SeventhOfTheNight -> R.string.high_lat_seventh_desc
        HighLatitudeOption.TwilightAngle -> R.string.high_lat_twilight_desc
    }
