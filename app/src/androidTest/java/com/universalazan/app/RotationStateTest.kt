package com.universalazan.app

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.location.MANUAL_LATITUDE_TAG
import com.universalazan.app.ui.location.MANUAL_LONGITUDE_TAG
import com.universalazan.app.ui.location.TIME_ZONE_SEARCH_TAG
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.SETTINGS_AZAN_TAG
import com.universalazan.app.ui.settings.SETTINGS_CALCULATION_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Rotating the phone makes Android rebuild the screen ("recreate"). These tests check that
 * nothing the user was doing is lost when that happens.
 */
@RunWith(AndroidJUnit4::class)
class RotationStateTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule)

    private fun rotate() {
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    private fun openLocationScreen() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Choose location").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Choose location").performScrollTo().performClick()
    }

    @Test
    fun typedCoordinatesSurviveRotation() {
        openLocationScreen()
        composeRule.onNodeWithTag(MANUAL_LATITUDE_TAG).performScrollTo().performTextInput("21.4225")
        composeRule.onNodeWithTag(MANUAL_LONGITUDE_TAG).performScrollTo().performTextInput("39.8262")
        rotate()
        composeRule.onNodeWithTag(MANUAL_LATITUDE_TAG).performScrollTo().assertTextContains("21.4225")
        composeRule.onNodeWithTag(MANUAL_LONGITUDE_TAG).performScrollTo().assertTextContains("39.8262")
    }

    @Test
    fun timeZonePickerStaysOpenWithItsSearch() {
        openLocationScreen()
        composeRule.onNodeWithText("Choose time zone for these coordinates").performScrollTo().performClick()
        composeRule.onNodeWithTag(TIME_ZONE_SEARCH_TAG).performTextInput("Karachi")
        rotate()
        composeRule.onNodeWithTag(TIME_ZONE_SEARCH_TAG).assertTextContains("Karachi")
        composeRule.onNodeWithText("Asia/Karachi").assertExists()
    }

    @Test
    fun openSettingsScreenStaysOpen() {
        composeRule.onNodeWithTag(navTestTag(AppDestination.Settings)).performClick()
        composeRule.onNodeWithTag(SETTINGS_CALCULATION_TAG).performClick()
        composeRule.onNodeWithText("Calculation method").assertExists()
        rotate()
        composeRule.onNodeWithText("Calculation method").assertExists()
    }

    @Test
    fun azanKeepsPlayingThroughRotation() {
        composeRule.onNodeWithTag(navTestTag(AppDestination.Settings)).performClick()
        composeRule.onNodeWithTag(SETTINGS_AZAN_TAG).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Play test sound for Fajr").performScrollTo().performClick()
        assertEquals(PlaybackState.Playing(Prayer.Fajr, AzanSound.Full), testAzanPlayer().state.value)
        rotate()
        assertEquals("rotation must not stop the Azan", PlaybackState.Playing(Prayer.Fajr, AzanSound.Full), testAzanPlayer().state.value)
        composeRule.onNodeWithContentDescription("Stop sound for Fajr").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { testAzanPlayer().state.value == PlaybackState.Idle }
    }
}
