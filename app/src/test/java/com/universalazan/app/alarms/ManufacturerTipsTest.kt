package com.universalazan.app.alarms

import com.universalazan.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ManufacturerTipsTest {
    @Test
    fun tipsMatchPhoneMakers() {
        assertEquals(R.string.alarms_tip_samsung, manufacturerAlarmTip("samsung"))
        assertEquals(R.string.alarms_tip_samsung, manufacturerAlarmTip(" Samsung "))
        assertEquals(R.string.alarms_tip_xiaomi, manufacturerAlarmTip("Xiaomi"))
        assertEquals(R.string.alarms_tip_xiaomi, manufacturerAlarmTip("POCO"))
        assertEquals(R.string.alarms_tip_huawei, manufacturerAlarmTip("HONOR"))
        assertEquals(R.string.alarms_tip_oppo, manufacturerAlarmTip("OnePlus"))
        assertEquals(R.string.alarms_tip_vivo, manufacturerAlarmTip("vivo"))
    }

    @Test
    fun otherPhonesGetTheGeneralTip() {
        assertEquals(R.string.alarms_tip_general, manufacturerAlarmTip("Google"))
        assertEquals(R.string.alarms_tip_general, manufacturerAlarmTip(""))
    }
}
