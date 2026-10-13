package com.universalazan.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Proves the accessibility checks in AccessibilityTest really run: a known-bad screen must fail. */
@RunWith(AndroidJUnit4::class)
class AccessibilityCheckerSanityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tinyUnlabelledButtonIsCaught() {
        composeRule.enableAccessibilityChecks()
        composeRule.setContent {
            // 16dp is far below the 48dp minimum touch target, and it has no label.
            Box(modifier = Modifier.size(16.dp).clickable {})
        }
        try {
            composeRule.onRoot().tryPerformAccessibilityChecks()
            fail("The accessibility checker did not report a 16dp unlabelled button")
        } catch (e: Throwable) {
            if (e is AssertionError && e.message?.startsWith("The accessibility checker did not") == true) throw e
            val message = e.message.orEmpty()
            assertTrue("unexpected error: $message", message.contains("touch", ignoreCase = true) || message.contains("label", ignoreCase = true))
        }
    }
}
