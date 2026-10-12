package com.universalazan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.ui.location.CITY_SEARCH_TAG
import com.universalazan.app.ui.location.MANUAL_LATITUDE_TAG
import com.universalazan.app.ui.location.MANUAL_LONGITUDE_TAG
import com.universalazan.app.ui.location.MANUAL_NAME_TAG
import com.universalazan.app.ui.location.MANUAL_SAVE_TAG
import com.universalazan.app.ui.location.MANUAL_TIME_ZONE_TAG
import com.universalazan.app.ui.location.TIME_ZONE_SEARCH_TAG
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: choosing a location by city and by coordinates. */
@RunWith(AndroidJUnit4::class)
class LocationFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ClearSavedLocationRule()).around(composeRule)

    private fun waitForText(text: String, timeoutMs: Long = 5_000) = composeRule.waitUntil(timeoutMs) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openLocationScreen() {
        waitForText("Choose location")
        composeRule.onNodeWithText("Choose location").performScrollTo().performClick()
        composeRule.onNodeWithText("Search for a city").assertIsDisplayed()
    }

    @Test
    fun manualCoordinatesAreSavedAndShownOnPrayerTimes() {
        openLocationScreen()
        composeRule.onNodeWithTag(MANUAL_NAME_TAG).performScrollTo().performTextInput("Test Place")
        composeRule.onNodeWithTag(MANUAL_LATITUDE_TAG).performScrollTo().performTextInput("21.4225")
        composeRule.onNodeWithTag(MANUAL_LONGITUDE_TAG).performScrollTo().performTextInput("39.8262")
        composeRule.onNodeWithTag(MANUAL_SAVE_TAG).performScrollTo().performClick()

        waitForText("Test Place")
        composeRule.onNodeWithTag(navTestTag(AppDestination.PrayerTimes)).assertIsSelected()
        composeRule.onNodeWithText("21.4225° N, 39.8262° E").assertIsDisplayed()
        val saved = runBlocking { testLocationRepository().savedLocation.first() }
        assertEquals(LocationSource.Manual, saved?.source)
    }

    @Test
    fun invalidCoordinatesShowErrorsAndAreNotSaved() {
        openLocationScreen()
        composeRule.onNodeWithTag(MANUAL_LATITUDE_TAG).performScrollTo().performTextInput("95")
        composeRule.onNodeWithTag(MANUAL_LONGITUDE_TAG).performScrollTo().performTextInput("abc")
        composeRule.onNodeWithTag(MANUAL_SAVE_TAG).performScrollTo().performClick()

        composeRule.onNodeWithText("Latitude must be between -90 and 90.").assertExists()
        composeRule.onNodeWithText("Enter a decimal number, such as 51.5074 or -0.1278.").assertExists()
        composeRule.onNodeWithText("Search for a city").assertExists()
        assertNull(runBlocking { testLocationRepository().savedLocation.first() })
    }

    @Test
    fun citySearchFindsMakkahFromMecca() {
        openLocationScreen()
        composeRule.onNodeWithTag(CITY_SEARCH_TAG).performTextInput("Mecca")
        waitForText("Makkah", timeoutMs = 15_000)
        composeRule.onAllNodesWithText("Makkah").onFirst().performScrollTo().performClick()

        waitForText("Time zone: Asia/Riyadh")
        composeRule.onNodeWithText("Makkah").assertIsDisplayed()
        val saved = runBlocking { testLocationRepository().savedLocation.first() }
        assertEquals(LocationSource.City, saved?.source)
        assertEquals("Asia/Riyadh", saved?.timeZoneId)
    }

    @Test
    fun manualTimeZoneCanBeChosen() {
        openLocationScreen()
        composeRule.onNodeWithText("Change time zone").performScrollTo().performClick()
        composeRule.onNodeWithTag(TIME_ZONE_SEARCH_TAG).performTextInput("Tokyo")
        composeRule.onNodeWithText("Asia/Tokyo").performClick()
        composeRule.onNodeWithTag(MANUAL_TIME_ZONE_TAG).performScrollTo().assertTextEquals("Asia/Tokyo")
    }

    @Test
    fun savedLocationSurvivesActivityRecreation() {
        runBlocking {
            testLocationRepository().save(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
            )
        }
        waitForText("London")
        composeRule.activityRule.scenario.recreate()
        waitForText("London")
        composeRule.onNodeWithText("Time zone: Europe/London").assertIsDisplayed()
    }
}
