package com.universalazan.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.SETTINGS_CALCULATION_TAG
import com.universalazan.app.ui.settings.choiceTestTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: real times appear, and changing settings updates them. */
@RunWith(AndroidJUnit4::class)
class PrayerTimesFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule).around(AppReadyRule(composeRule))

    private fun waitForText(text: String, substring: Boolean = false) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun saveLocation(location: SavedLocation) = runBlocking { testLocationRepository().save(location) }

    /** All text of the merged "Asr" row, e.g. "Asr, 3:35 PM". */
    private fun asrRowText(): String {
        val node = composeRule.onNode(hasText("Asr")).performScrollTo().fetchSemanticsNode()
        return node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
    }

    @Test
    fun savedCityShowsCalculatedTimes() {
        saveLocation(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        waitForText("Method: Muslim World League · Asr: Standard")
        assertEquals(0, composeRule.onAllNodesWithText("--:--", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun switchingToHanafiChangesAsr() {
        saveLocation(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        waitForText("Method: Muslim World League · Asr: Standard")
        val standardAsr = asrRowText()

        composeRule.onNodeWithTag(navTestTag(AppDestination.Settings)).performClick()
        composeRule.onNodeWithTag(SETTINGS_CALCULATION_TAG).performClick()
        composeRule.onNodeWithTag(choiceTestTag(AsrMadhab.Hanafi)).performScrollTo().performClick()
        composeRule.onNodeWithTag(choiceTestTag(AsrMadhab.Hanafi)).assertIsSelected()
        assertEquals(AsrMadhab.Hanafi, runBlocking { testCalculationSettingsRepository().settings.first().asrMadhab })

        composeRule.onNodeWithTag(navTestTag(AppDestination.PrayerTimes)).performClick()
        waitForText("Method: Muslim World League · Asr: Hanafi")
        assertNotEquals(standardAsr, asrRowText())
    }

    @Test
    fun changingCityChangesTimes() {
        saveLocation(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        waitForText("Time zone: Europe/London")
        val londonAsr = asrRowText()
        saveLocation(SavedLocation("Makkah", 21.4266, 39.8256, "Asia/Riyadh", LocationSource.City))
        waitForText("Time zone: Asia/Riyadh")
        assertNotEquals(londonAsr, asrRowText())
    }

    @Test
    fun calculationSettingsOpenFromPrayerTimes() {
        saveLocation(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        waitForText("Calculation settings")
        composeRule.onNodeWithText("Calculation settings").performScrollTo().performClick()
        composeRule.onNodeWithText("Calculation method").assertExists()
        composeRule.onNodeWithTag(navTestTag(AppDestination.PrayerTimes)).assertIsSelected()
    }
}
