package com.universalazan.app

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import com.universalazan.app.ui.qibla.QIBLA_BEARING_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: the Qibla screen shows the calculated bearing. */
@RunWith(AndroidJUnit4::class)
class QiblaFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppSettingsRule()).around(composeRule).around(AppReadyRule(composeRule))

    private fun waitForText(text: String, substring: Boolean = false) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun save(location: SavedLocation) = runBlocking { testLocationRepository().save(location) }

    private fun openQibla() = composeRule.onNodeWithTag(navTestTag(AppDestination.Qibla)).performClick()

    @Test
    fun showsBearingForSavedLocation() {
        save(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        openQibla()
        waitForText("For London")
        // Reference (AlAdhan): 118.987°, shown to one decimal place.
        composeRule.onNodeWithTag(QIBLA_BEARING_TAG).assertContentDescriptionEquals("Qibla direction 119.0 degrees, ESE")
        composeRule.onNodeWithText("Clockwise from true north (ESE)").assertIsDisplayed()
        composeRule.onNodeWithText("km to the Kaaba in Makkah", substring = true).assertIsDisplayed()
    }

    @Test
    fun bearingUpdatesWhenLocationChanges() {
        save(SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City))
        openQibla()
        waitForText("For London")
        save(SavedLocation("Sydney", -33.8688, 151.2093, "Australia/Sydney", LocationSource.City))
        waitForText("For Sydney")
        // Reference (AlAdhan): 277.500°.
        composeRule.onNodeWithTag(QIBLA_BEARING_TAG).assertContentDescriptionEquals("Qibla direction 277.5 degrees, W")
    }

    @Test
    fun noLocationAsksTheUserToChooseOne() {
        openQibla()
        waitForText("No location selected")
        composeRule.onNodeWithText("Choose location").performScrollTo().performClick()
        composeRule.onNodeWithText("Search for a city").assertIsDisplayed()
    }

    @Test
    fun atTheKaabaExplainsThereIsNoSingleDirection() {
        save(SavedLocation("Kaaba", 21.4225, 39.8262, "Asia/Riyadh", LocationSource.Manual))
        openQibla()
        waitForText("You are at the Kaaba", substring = true)
    }
}
