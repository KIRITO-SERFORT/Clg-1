package com.example

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test

/** Basic launch regression: verifies the real Activity renders on an emulator/device. */
class MainActivitySmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun mainActivityLaunchesAndRendersClgOnePercentBranding() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Clg 1%").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Clg 1%").assertExists()
    }
}
