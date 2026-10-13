package com.universalazan.app.ui.prayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalazan.app.R
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.ui.theme.UniversalAzanTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Checks each state of the Prayer Times screen renders correctly. */
@RunWith(AndroidJUnit4::class)
class PrayerTimesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: PrayerTimesUiState, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            UniversalAzanTheme {
                PrayerTimesScreen(state = state, onChooseLocation = {}, onRetry = onRetry)
            }
        }
    }

    @Test
    fun readyStateShowsAllPrayerNamesAndNoRealTimes() {
        show(PrayerTimesUiState.Ready(null, LocalDate.of(2026, 10, 12), notCalculatedPrayerRows()))

        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
            composeRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("For information, not a prayer").assertExists()
        // Every one of the six rows shows the empty "--:--" placeholder.
        assertEquals(
            6,
            composeRule.onAllNodesWithText("--:--", useUnmergedTree = true).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun readyStateShowsSelectedLocationName() {
        show(
            PrayerTimesUiState.Ready(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
                LocalDate.of(2026, 10, 12),
                notCalculatedPrayerRows(),
            ),
        )
        composeRule.onNodeWithText("London").assertIsDisplayed()
        composeRule.onNodeWithText("51.5085° N, 0.1257° W").assertIsDisplayed()
        composeRule.onNodeWithText("Time zone: Europe/London").assertIsDisplayed()
        composeRule.onNodeWithText("Change location").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculatedStateShowsTimesNextPrayerAndMethod() {
        show(
            PrayerTimesViewModel.buildState(
                SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City),
                CalculationSettings(),
                // 13 Oct 2026, 14:00 in London: next prayer is Asr.
                Instant.parse("2026-10-13T13:00:00Z"),
            ),
        )
        assertEquals(0, composeRule.onAllNodesWithText("--:--", useUnmergedTree = true).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Next").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Method: Muslim World League · Asr: Standard").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun polarProblemIsExplained() {
        show(
            PrayerTimesViewModel.buildState(
                SavedLocation("Tromsø", 69.6489, 18.9551, "Europe/Oslo", LocationSource.City),
                CalculationSettings(),
                Instant.parse("2026-12-21T12:00:00Z"),
            ),
        )
        composeRule.onNodeWithText("the sun does not rise or does not set", substring = true).assertIsDisplayed()
        assertEquals(6, composeRule.onAllNodesWithText("--:--", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun loadingStateShowsLoadingMessage() {
        show(PrayerTimesUiState.Loading)
        composeRule.onNodeWithText("Loading prayer times…").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetryWorks() {
        var retried = false
        show(PrayerTimesUiState.Error(R.string.error_prayer_times), onRetry = { retried = true })
        composeRule.onNodeWithText("Prayer times could not be loaded.").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }
}
