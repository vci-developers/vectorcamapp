package com.vci.vectorcamapp.landing

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.vci.vectorcamapp.landing.presentation.LandingAction
import com.vci.vectorcamapp.landing.presentation.LandingScreen
import com.vci.vectorcamapp.landing.presentation.LandingState
import org.junit.Rule
import org.junit.Test

class LandingScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val dummyProgramName = "My Program"
    private val dummyCountryName = "My Country"

    @Test fun header_showsProgramName() {
        val state = LandingState(enrolledProgram = com.vci.vectorcamapp.core.domain.model.Program(0, dummyProgramName, dummyCountryName))
        rule.setContent {
            LandingScreen(state = state, onAction = {}, modifier = Modifier)
        }
        rule.onNodeWithText("Program: $dummyProgramName")
            .assertIsDisplayed()
    }

    @Test fun incompleteBadge_showsCount() {
        val state = LandingState(
            enrolledProgram = com.vci.vectorcamapp.core.domain.model.Program(0, "", ""),
            incompleteSessionsCount = 3
        )
        rule.setContent {
            LandingScreen(state = state, onAction = {}, modifier = Modifier)
        }
        rule.onNode(hasText("3") and hasParent(hasContentDescription("Action Icon")))
            .assertIsDisplayed()
    }

    @Test fun clickingTiles_triggersCallbacks() {
        var lastAction: LandingAction? = null
        val state = LandingState(
            enrolledProgram = com.vci.vectorcamapp.core.domain.model.Program(0, "", ""),
            incompleteSessionsCount = 0
        )

        rule.setContent {
            LandingScreen(state = state, onAction = { lastAction = it })
        }

        rule.onNodeWithText("New Surveillance Session").performClick()
        assert(lastAction == LandingAction.StartNewSurveillanceSession)

        rule.onNodeWithText("Data Collection Mode").performClick()
        assert(lastAction == LandingAction.StartNewDataCollectionSession)

        rule.onNodeWithText("View Incomplete Sessions").performClick()
        assert(lastAction == LandingAction.ViewIncompleteSessions)

        rule.onNodeWithText("View Complete Sessions").performClick()
        assert(lastAction == LandingAction.ViewCompleteSessions)
    }

    @Test fun showResumeDialog_whenFlagTrue() {
        var lastAction: LandingAction? = null
        val state = LandingState(
            enrolledProgram = com.vci.vectorcamapp.core.domain.model.Program(0, "", ""),
            showResumeDialog = true
        )

        rule.setContent {
            LandingScreen(state = state, onAction = { lastAction = it })
        }

        rule.onNodeWithText("Resume unfinished session?").assertIsDisplayed()
        rule.onNodeWithText("Yes, resume").performClick()
        assert(lastAction == LandingAction.ResumeSession)

        // reset and test "No, start new"
        lastAction = null
        rule.onNodeWithText("No, start new").performClick()
        assert(lastAction == LandingAction.DismissResumePrompt)
    }
}
