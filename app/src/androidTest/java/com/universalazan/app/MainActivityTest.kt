package com.universalazan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Runs on an emulator or phone: checks the app launches and shows its title. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(AppReadyRule(composeRule))

    @Test
    fun packageNameIsCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.universalazan.app", appContext.packageName)
    }

    @Test
    fun launchShowsAppTitle() {
        composeRule.onNodeWithText("Universal Azan & Qibla").assertIsDisplayed()
    }
}
