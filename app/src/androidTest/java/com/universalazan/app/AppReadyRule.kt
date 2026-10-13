package com.universalazan.app

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.universalazan.app.ui.navigation.AppDestination
import com.universalazan.app.ui.navigation.navTestTag
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Waits until the app has drawn its first screen before a test starts.
 *
 * On start-up the app reads the saved theme before drawing anything (so it never flashes in the
 * wrong colours). That read happens in the background, which the test framework does not wait
 * for, so on a slow or busy device a test could otherwise tap before the screen exists.
 */
class AppReadyRule(private val composeRule: ComposeTestRule) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithTag(navTestTag(AppDestination.PrayerTimes), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            base.evaluate()
        }
    }
}

/** Waits until the app has drawn its screen again, e.g. after the activity was recreated (rotation). */
fun ComposeTestRule.waitForAppReady() {
    waitUntil(timeoutMillis = 15_000) {
        onAllNodesWithTag(navTestTag(AppDestination.PrayerTimes), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    waitForIdle()
}
