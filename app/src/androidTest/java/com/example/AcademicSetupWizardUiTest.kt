package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AcademicProfile
import com.example.ui.screens.AcademicSetupWizard
import com.example.ui.screens.AcademicWizardResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** Runs on an emulator/device as part of connectedAndroidTest. */
class AcademicSetupWizardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wizardNavigatesAllFourStepsAndSavesValidatedProfile() {
        var result: AcademicWizardResult? = null
        compose.setContent {
            MaterialTheme {
                AcademicSetupWizard(
                    initialCollege = "Example Institute",
                    initialTerm = "Semester 1",
                    initialPassingPercent = 40.0,
                    onDismiss = {},
                    onSaveDraft = {},
                    onComplete = { result = it }
                )
            }
        }

        compose.onNodeWithText("Academic setup · Step 1 of 4").assertIsDisplayed()
        compose.onNodeWithText("Academic year (e.g. 2026–27)").performTextInput("2026-27")
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Academic setup · Step 2 of 4").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Academic setup · Step 3 of 4").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Academic setup · Step 4 of 4").assertIsDisplayed()
        compose.onNodeWithText("Save setup").performClick()

        compose.runOnIdle {
            assertNotNull(result)
            assertEquals("Example Institute", result!!.profile.institution)
            assertEquals("2026-27", result!!.profile.academicYear)
            assertEquals(2, result!!.profile.defaultScheme.components.size)
        }
    }

    @Test fun incompleteWizardCanSaveDraftWithoutNonFiniteGradingValues() {
        var draft: AcademicProfile? = null
        compose.setContent {
            MaterialTheme {
                AcademicSetupWizard(
                    initialCollege = "Example Institute",
                    initialTerm = "Semester 1",
                    initialPassingPercent = 40.0,
                    onDismiss = {},
                    onSaveDraft = { draft = it },
                    onComplete = {}
                )
            }
        }
        compose.onNodeWithText("Save draft").performClick()
        compose.runOnIdle {
            assertNotNull(draft)
            assertEquals(true, draft!!.grading.passingPercent.isFinite())
            assertEquals(2, draft!!.grading.decimalPlaces)
        }
        compose.onNodeWithText("Draft saved securely on this device. Reopen the wizard to continue.").assertIsDisplayed()
    }
}
