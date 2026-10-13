package com.universalazan.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
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
import com.universalazan.app.data.preferences.ThemeSetting
import com.universalazan.app.data.preferences.TimeFormatSetting
import com.universalazan.app.domain.prayer.AdjustableTime
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.ABOUT_VERSION_TAG
import com.universalazan.app.ui.settings.SETTINGS_ABOUT_TAG
import com.universalazan.app.ui.settings.SETTINGS_CALCULATION_TAG
import com.universalazan.app.ui.settings.adjustmentTag
import com.universalazan.app.ui.settings.generalChipTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalTime

/** Runs on an emulator or phone: Stage 9 settings (adjustments, time format, theme, About). */
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule)

    private val london = SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City)

    private fun waitForText(text: String, substring: Boolean = false) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun nav(destination: AppDestination) = composeRule.onNodeWithTag(navTestTag(destination)).performClick()

    /** The time text of a prayer row on Prayer Times, e.g. "05:30". */
    private fun rowTime(name: String): String {
        val node = composeRule.onNode(hasText(name)).performScrollTo().fetchSemanticsNode()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return texts.first { it.matches(Regex("""\d{1,2}:\d{2}.*""")) }
    }

    @Test
    fun twentyFourHourFormatIsUsed() {
        runBlocking {
            testLocationRepository().save(london)
            testGeneralSettingsRepository().setTimeFormat(TimeFormatSetting.TwentyFourHour)
        }
        waitForText("Time zone: Europe/London")
        composeRule.waitUntil(5_000) { rowTime("Isha").matches(Regex("""\d{2}:\d{2}""")) }
        assertFalse(rowTime("Isha").contains("M"))

        runBlocking { testGeneralSettingsRepository().setTimeFormat(TimeFormatSetting.TwelveHour) }
        composeRule.waitUntil(5_000) { rowTime("Isha").endsWith("PM") }
    }

    @Test
    fun fajrAdjustmentMovesTheTimeAndTheAlarm() {
        runBlocking {
            testLocationRepository().save(london)
            testGeneralSettingsRepository().setTimeFormat(TimeFormatSetting.TwentyFourHour)
        }
        waitForText("Time zone: Europe/London")
        val before = LocalTime.parse(rowTime("Fajr"))
        val alarmBefore = runBlocking { testAlarmCoordinator().reschedule() }.first { it.prayer == Prayer.Fajr }

        nav(AppDestination.Settings)
        composeRule.onNodeWithTag(SETTINGS_CALCULATION_TAG).performClick()
        repeat(3) {
            composeRule.onNodeWithTag(adjustmentTag(AdjustableTime.Fajr, "plus")).performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(adjustmentTag(AdjustableTime.Fajr, "value")).assertTextEquals("+3 min")
        composeRule.waitUntil(5_000) { runBlocking { testCalculationSettingsRepository().settings.first().adjustments.fajr } == 3 }

        nav(AppDestination.PrayerTimes)
        composeRule.waitUntil(5_000) { LocalTime.parse(rowTime("Fajr")) != before }
        assertEquals(3, Duration.between(before, LocalTime.parse(rowTime("Fajr"))).toMinutes())

        val alarmAfter = runBlocking { testAlarmCoordinator().reschedule() }.first { it.prayer == Prayer.Fajr }
        assertEquals(3, Duration.between(alarmBefore.time, alarmAfter.time).toMinutes())
    }

    @Test
    fun themeChoiceIsSaved() {
        nav(AppDestination.Settings)
        composeRule.onNodeWithTag(generalChipTag(ThemeSetting.Dark)).performScrollTo().performClick()
        composeRule.onNodeWithTag(generalChipTag(ThemeSetting.Dark)).assertIsSelected()
        composeRule.waitUntil(5_000) { runBlocking { testGeneralSettingsRepository().settings.first().theme } == ThemeSetting.Dark }
        // Still selected after the screen is recreated (e.g. rotation).
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag(generalChipTag(ThemeSetting.Dark)).performScrollTo().assertIsSelected()
    }

    @Test
    fun aboutShowsVersionAndPrivacy() {
        nav(AppDestination.Settings)
        composeRule.onNodeWithTag(SETTINGS_ABOUT_TAG).performScrollTo().performClick()
        composeRule.onNodeWithTag(ABOUT_VERSION_TAG).assertTextEquals("Version 0.1.0")
        composeRule.onNodeWithText("no internet permission", substring = true).performScrollTo()
        assertTrue(composeRule.onAllNodesWithText("GeoNames", substring = true).fetchSemanticsNodes().isNotEmpty())
    }
}
