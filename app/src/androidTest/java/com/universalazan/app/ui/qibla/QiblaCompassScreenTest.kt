package com.universalazan.app.ui.qibla

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.sensors.CompassAccuracy
import com.universalazan.app.ui.theme.UniversalAzanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Qibla screen with simulated compass readings. London's Qibla bearing is 119.0°. */
@RunWith(AndroidJUnit4::class)
class QiblaCompassScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val london = QiblaViewModel.stateFor(
        SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City),
    )

    private fun show(compass: CompassUiState) {
        composeRule.setContent {
            UniversalAzanTheme {
                QiblaScreen(state = london, compass = compass, onChooseLocation = {})
            }
        }
    }

    private fun live(heading: Double, accuracy: CompassAccuracy = CompassAccuracy.High, interference: Boolean = false) =
        CompassUiState.Live(trueHeading = heading, accuracy = accuracy, interference = interference, declination = 0.8)

    @Test
    fun tellsTheUserToTurnRight() {
        show(live(heading = 90.0))
        composeRule.onNodeWithTag(COMPASS_INSTRUCTION_TAG).performScrollTo().assertTextEquals("Turn right 29°")
        composeRule.onNodeWithContentDescription("Compass. Turn right 29°").assertExists()
        composeRule.onNodeWithText("Phone pointing: 90° (E)").assertExists()
    }

    @Test
    fun tellsTheUserToTurnLeft() {
        show(live(heading = 200.0))
        composeRule.onNodeWithTag(COMPASS_INSTRUCTION_TAG).performScrollTo().assertTextEquals("Turn left 81°")
    }

    @Test
    fun confirmsFacingTheQibla() {
        show(live(heading = 117.0))
        composeRule.onNodeWithTag(COMPASS_INSTRUCTION_TAG).performScrollTo().assertTextEquals("You are facing the Qibla")
    }

    @Test
    fun noSensorShowsExplanationNotAFakeCompass() {
        show(CompassUiState.Unavailable)
        composeRule.onNodeWithText("No compass sensor").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(COMPASS_INSTRUCTION_TAG).assertDoesNotExist()
        // The calculated bearing is still shown.
        composeRule.onNodeWithTag(QIBLA_BEARING_TAG).assertExists()
    }

    @Test
    fun lowAccuracyAsksForCalibration() {
        show(live(heading = 90.0, accuracy = CompassAccuracy.Unreliable))
        composeRule.onNodeWithText("figure-of-eight", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun interferenceIsReported() {
        show(live(heading = 90.0, interference = true))
        composeRule.onNodeWithText("Strong magnetic interference", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun waitingShowsProgress() {
        show(CompassUiState.Waiting)
        composeRule.onNodeWithText("Starting the compass…").performScrollTo().assertIsDisplayed()
    }
}
