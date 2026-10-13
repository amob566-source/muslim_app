package com.universalazan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.SETTINGS_LOCATION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: checks all sections are reachable. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule).around(AppReadyRule(composeRule))

    private fun nav(destination: AppDestination) = composeRule.onNodeWithTag(navTestTag(destination))

    private fun waitForText(text: String) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun startsOnPrayerTimes() {
        nav(AppDestination.PrayerTimes).assertIsSelected()
        waitForText("No location selected")
    }

    @Test
    fun canReachQiblaAndSettingsAndComeBack() {
        nav(AppDestination.Qibla).performClick()
        composeRule.onNodeWithText("Qibla direction").assertIsDisplayed()

        nav(AppDestination.Settings).performClick()
        composeRule.onNodeWithText("Prayer calculation").assertIsDisplayed()

        nav(AppDestination.PrayerTimes).performClick()
        waitForText("No location selected")
    }

    @Test
    fun chooseLocationOpensLocationScreenAndBackCloses() {
        waitForText("Choose location")
        composeRule.onNodeWithText("Choose location").performScrollTo().performClick()
        composeRule.onNodeWithText("Search for a city").assertIsDisplayed()

        pressBack()
        waitForText("No location selected")
        nav(AppDestination.PrayerTimes).assertIsSelected()
    }

    @Test
    fun settingsLocationRowOpensLocationScreen() {
        nav(AppDestination.Settings).performClick()
        composeRule.onNodeWithTag(SETTINGS_LOCATION_TAG).performClick()
        composeRule.onNodeWithText("Search for a city").assertIsDisplayed()
    }

    @Test
    fun backFromSettingsReturnsToPrayerTimes() {
        nav(AppDestination.Settings).performClick()
        // Wait until Settings is shown (and the back handler is active) before pressing back.
        nav(AppDestination.Settings).assertIsSelected()
        pressBack()
        nav(AppDestination.PrayerTimes).assertIsSelected()
    }

    @Test
    fun selectedSectionSurvivesActivityRecreation() {
        nav(AppDestination.Qibla).performClick()
        // Same as what happens when the screen is rotated.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForAppReady()
        nav(AppDestination.Qibla).assertIsSelected()
        composeRule.onNodeWithText("Qibla direction").assertIsDisplayed()
    }
}
