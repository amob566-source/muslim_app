package com.universalazan.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.settings.SETTINGS_AZAN_TAG
import com.universalazan.app.ui.settings.soundChipTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: choosing and testing Azan sounds in Settings. */
@RunWith(AndroidJUnit4::class)
class AzanSoundsFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule)

    private fun openAzanSounds() {
        composeRule.onNodeWithTag(navTestTag(AppDestination.Settings)).performClick()
        composeRule.onNodeWithTag(SETTINGS_AZAN_TAG).performScrollTo().performClick()
    }

    @Test
    fun choosingSilentIsSavedAndDisablesTest() {
        openAzanSounds()
        composeRule.onNodeWithTag(soundChipTag(Prayer.Asr, AzanSound.Silent)).performScrollTo().performClick()
        composeRule.onNodeWithTag(soundChipTag(Prayer.Asr, AzanSound.Silent)).assertIsSelected()
        composeRule.onNodeWithContentDescription("Play test sound for Asr").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Play test sound for Dhuhr").assertIsEnabled()
        composeRule.waitUntil(5_000) {
            runBlocking { testAzanSoundRepository().settings.first().soundFor(Prayer.Asr) } == AzanSound.Silent
        }
    }

    @Test
    fun choiceSurvivesActivityRecreation() {
        openAzanSounds()
        composeRule.onNodeWithTag(soundChipTag(Prayer.Dhuhr, AzanSound.Short)).performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { testAzanSoundRepository().settings.first().soundFor(Prayer.Dhuhr) } == AzanSound.Short
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag(soundChipTag(Prayer.Dhuhr, AzanSound.Short)).performScrollTo().assertIsSelected()
    }

    @Test
    fun testButtonPlaysAndStopButtonStops() {
        openAzanSounds()
        composeRule.onNodeWithContentDescription("Play test sound for Fajr").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Stop sound for Fajr").assertExists()
        assertEquals(PlaybackState.Playing(Prayer.Fajr, AzanSound.Full), testAzanPlayer().state.value)
        composeRule.onNodeWithContentDescription("Stop sound for Fajr").performClick()
        composeRule.waitUntil(5_000) { testAzanPlayer().state.value == PlaybackState.Idle }
    }
}
