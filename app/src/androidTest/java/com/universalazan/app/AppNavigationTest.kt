package com.universalazan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator or phone: checks all three sections are reachable. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun nav(destination: AppDestination) = composeRule.onNodeWithTag(navTestTag(destination))

    @Test
    fun startsOnPrayerTimes() {
        nav(AppDestination.PrayerTimes).assertIsSelected()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()
    }

    @Test
    fun canReachQiblaAndSettingsAndComeBack() {
        nav(AppDestination.Qibla).performClick()
        composeRule.onNodeWithText("Qibla direction").assertIsDisplayed()

        nav(AppDestination.Settings).performClick()
        composeRule.onNodeWithText("Prayer calculation").assertIsDisplayed()

        nav(AppDestination.PrayerTimes).performClick()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()
    }

    @Test
    fun chooseLocationOpensSettings() {
        composeRule.onNodeWithText("Choose location").performScrollTo().performClick()
        nav(AppDestination.Settings).assertIsSelected()
    }

    @Test
    fun backFromSettingsReturnsToPrayerTimes() {
        nav(AppDestination.Settings).performClick()
        // Wait until Settings is shown (and the back handler is active) before pressing back.
        nav(AppDestination.Settings).assertIsSelected()
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        nav(AppDestination.PrayerTimes).assertIsSelected()
    }

    @Test
    fun selectedSectionSurvivesActivityRecreation() {
        nav(AppDestination.Qibla).performClick()
        // Same as what happens when the screen is rotated.
        composeRule.activityRule.scenario.recreate()
        nav(AppDestination.Qibla).assertIsSelected()
        composeRule.onNodeWithText("Qibla direction").assertIsDisplayed()
    }
}
