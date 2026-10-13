package com.universalazan.app.alarms

import androidx.annotation.StringRes
import com.universalazan.app.R
import java.util.Locale

/**
 * Some phone makers add their own battery savers that can stop alarms. Returns the tip for this
 * phone's maker (from android.os.Build.MANUFACTURER), or a general tip.
 */
@StringRes
fun manufacturerAlarmTip(manufacturer: String): Int = when (manufacturer.trim().lowercase(Locale.ROOT)) {
    "samsung" -> R.string.alarms_tip_samsung
    "xiaomi", "redmi", "poco" -> R.string.alarms_tip_xiaomi
    "huawei", "honor" -> R.string.alarms_tip_huawei
    "oppo", "realme", "oneplus" -> R.string.alarms_tip_oppo
    "vivo", "iqoo" -> R.string.alarms_tip_vivo
    else -> R.string.alarms_tip_general
}
