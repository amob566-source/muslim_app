package com.universalazan.app

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.SETTINGS_ABOUT_TAG
import com.universalazan.app.ui.settings.SETTINGS_ALARMS_TAG
import com.universalazan.app.ui.settings.SETTINGS_AZAN_TAG
import com.universalazan.app.ui.settings.SETTINGS_CALCULATION_TAG
import com.universalazan.app.ui.settings.SETTINGS_LOCATION_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Runs Google's Accessibility Test Framework (the checks behind Android's Accessibility Scanner)
 * on every screen: missing labels, touch targets smaller than 48dp, low text contrast, etc.
 * A problem fails the test with a description of what and where.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule).around(AppReadyRule(composeRule))

    @Before
    fun setUp() {
        runBlocking { testLocationRepository().save(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City)) }
        composeRule.enableAccessibilityChecks()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Time zone: Europe/London").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun check() {
        composeRule.waitForIdle()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun nav(destination: AppDestination) {
        composeRule.onNodeWithTag(navTestTag(destination)).performClick()
        composeRule.waitForIdle()
    }

    private fun openFromSettings(tag: String) {
        nav(AppDestination.Settings)
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun prayerTimesScreen() = check()

    @Test
    fun qiblaScreen() {
        nav(AppDestination.Qibla)
        check()
    }

    @Test
    fun settingsScreen() {
        nav(AppDestination.Settings)
        check()
    }

    @Test
    fun locationScreen() {
        openFromSettings(SETTINGS_LOCATION_TAG)
        composeRule.onNodeWithText("Search for a city").assertExists()
        check()
    }

    @Test
    fun calculationSettingsScreen() {
        openFromSettings(SETTINGS_CALCULATION_TAG)
        check()
    }

    @Test
    fun azanSoundsScreen() {
        openFromSettings(SETTINGS_AZAN_TAG)
        check()
    }

    @Test
    fun prayerAlarmsScreen() {
        openFromSettings(SETTINGS_ALARMS_TAG)
        check()
    }

    @Test
    fun aboutScreen() {
        openFromSettings(SETTINGS_ABOUT_TAG)
        check()
    }

    @Test
    fun timeZonePicker() {
        openFromSettings(SETTINGS_LOCATION_TAG)
        composeRule.onNodeWithText("Change time zone").performScrollTo().performClick()
        composeRule.waitForIdle()
        check()
    }
}
