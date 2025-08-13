package com.vci.vectorcamapp.landing.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.landing.presentation.util.LandingTestTags
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LandingScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }


    private val dummyProgramName = "My Program"
    private val dummyCountryName = "My Country"

    // =========================
    // Harness / Helpers
    // =========================
    private fun launchLandingScreen(
        initialState: LandingState = LandingState(),
        onAction: ((LandingAction) -> Unit)? = null
    ) {
        composeRule.activity.setContent {
            VectorcamappTheme {
                BaseScaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LandingScreen(
                        state = initialState,
                        onAction = onAction ?: {},
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun clickTile(tag: String) {
        composeRule.onNodeWithTag(tag).assertExists().performClick()
    }

    // =========================
    // A. Static UI
    // =========================

    @Test
    fun landUi_a01_headerAndSectionsVisible() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, dummyProgramName, dummyCountryName)
            )
        )
        composeRule.onNodeWithText("Welcome to VectorCam!").assertIsDisplayed()
        composeRule.onNodeWithText("Program: $dummyProgramName").assertIsDisplayed()
        composeRule.onNodeWithTag(LandingTestTags.SECTION_IMAGING).assertExists()
        composeRule.onNodeWithTag(LandingTestTags.SECTION_LIBRARY).assertExists()
    }

    @Test
    fun landUi_a02_tileDescriptionsVisible() {
        launchLandingScreen(
            initialState = LandingState(enrolledProgram = Program(0, "", ""))
        )
        composeRule.onNodeWithText("Begin a new household visit and capture mosquito images.").assertIsDisplayed()
        composeRule.onNodeWithText("Capture and upload mosquito images without filling forms.").assertIsDisplayed()
        composeRule.onNodeWithText("Resume and complete any unfinished sessions.").assertIsDisplayed()
        composeRule.onNodeWithText("Review fully completed sessions and uploaded data.").assertIsDisplayed()
    }

    @Test
    fun landUi_a03_rootScreenHasTag_andHeaderVisible() {
        launchLandingScreen(
            initialState = LandingState(enrolledProgram = Program(0, dummyProgramName, dummyCountryName))
        )
        composeRule.onNodeWithTag(LandingTestTags.SCREEN).assertExists()
        composeRule.onNodeWithText("Welcome to VectorCam!").assertIsDisplayed()
    }

    @Test
    fun landUi_a04_sectionHeadersTextPresent() {
        launchLandingScreen(initialState = LandingState(enrolledProgram = Program(0, "", "")))
        composeRule.onNodeWithText("Imaging").assertIsDisplayed()
        composeRule.onNodeWithText("Library").assertIsDisplayed()
    }

    // =========================
    // B. Tile Click Actions
    // =========================

    @Test
    fun landUi_b01_clickingTiles_invokesActions() {
        var lastAction: LandingAction? = null
        launchLandingScreen(
            initialState = LandingState(enrolledProgram = Program(0, "", "")),
            onAction = { lastAction = it }
        )

        clickTile(LandingTestTags.TILE_NEW_SURVEILLANCE)
        assert(lastAction == LandingAction.StartNewSurveillanceSession)

        clickTile(LandingTestTags.TILE_DATA_COLLECTION)
        assert(lastAction == LandingAction.StartNewDataCollectionSession)

        clickTile(LandingTestTags.TILE_INCOMPLETE)
        assert(lastAction == LandingAction.ViewIncompleteSessions)

        clickTile(LandingTestTags.TILE_COMPLETE)
        assert(lastAction == LandingAction.ViewCompleteSessions)
    }

    @Test
    fun landUi_b02_tilesHaveClickActions() {
        launchLandingScreen(initialState = LandingState(enrolledProgram = Program(0, "", "")))
        composeRule.onNodeWithTag(LandingTestTags.TILE_NEW_SURVEILLANCE).assertExists().assertHasClickAction()
        composeRule.onNodeWithTag(LandingTestTags.TILE_DATA_COLLECTION).assertExists().assertHasClickAction()
        composeRule.onNodeWithTag(LandingTestTags.TILE_INCOMPLETE).assertExists().assertHasClickAction()
        composeRule.onNodeWithTag(LandingTestTags.TILE_COMPLETE).assertExists().assertHasClickAction()
    }

    // =========================
    // C. Badge Visibility
    // =========================

    @Test
    fun landUi_c01_badgeVisible_whenCountPositive() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                incompleteSessionsCount = 3
            )
        )
        composeRule.onNodeWithTag(LandingTestTags.BADGE_INCOMPLETE, useUnmergedTree = true)
            .assertExists()

        composeRule.onNodeWithText("3", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("3")
    }


    @Test
    fun landUi_c02_badgeNotShown_whenZero() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                incompleteSessionsCount = 0
            )
        )
        composeRule.onNodeWithTag(LandingTestTags.BADGE_INCOMPLETE).assertDoesNotExist()
        composeRule.onAllNodesWithText("0", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun landUi_c03_badgeUpdatesAcrossRelaunch() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                incompleteSessionsCount = 0
            )
        )
        composeRule.onNodeWithTag(LandingTestTags.BADGE_INCOMPLETE).assertDoesNotExist()

        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                incompleteSessionsCount = 7
            )
        )
        composeRule.onNodeWithTag(LandingTestTags.BADGE_INCOMPLETE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("7", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("7")
    }

    // =========================
    // D. Resume Dialog
    // =========================

    @Test
    fun landUi_d01_resumeDialog_shown_whenFlagTrue_buttonsWork() {
        var lastAction: LandingAction? = null
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                showResumeDialog = true
            ),
            onAction = { lastAction = it }
        )

        composeRule.onNodeWithText("Resume unfinished session?").assertIsDisplayed()
        composeRule.onNodeWithTag(LandingTestTags.RESUME_CONFIRM)
            .assertIsEnabled()
            .performClick()
        assert(lastAction == LandingAction.ResumeSession)

        lastAction = null
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                showResumeDialog = true
            ),
            onAction = { lastAction = it }
        )
        composeRule.onNodeWithTag(LandingTestTags.RESUME_DISMISS)
            .assertIsEnabled()
            .performClick()
        assert(lastAction == LandingAction.DismissResumePrompt)
    }

    @Test
    fun landUi_d02_resumeDialog_notShown_whenFlagFalse() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                showResumeDialog = false
            )
        )
        composeRule.onNodeWithText("Resume unfinished session?").assertDoesNotExist()
    }

    // =========================
    // E. Edge Cases
    // =========================

    @Test
    fun landUi_d03_resumeDialog_hasTestTag_andButtonsEnabled() {
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, "", ""),
                showResumeDialog = true
            )
        )
        composeRule.onNodeWithTag(LandingTestTags.RESUME_DIALOG).assertExists()
        composeRule.onNodeWithText("Resume unfinished session?").assertIsDisplayed()
        composeRule.onNodeWithTag(LandingTestTags.RESUME_CONFIRM).assertIsEnabled()
        composeRule.onNodeWithTag(LandingTestTags.RESUME_DISMISS).assertIsEnabled()
        composeRule.onNodeWithText("Yes, resume").assertIsDisplayed()
        composeRule.onNodeWithText("No, start new").assertIsDisplayed()
    }

    @Test
    fun landUi_e01_handlesVeryLongProgramName_withoutCrashing() {
        val longName = "P".repeat(200)
        launchLandingScreen(
            initialState = LandingState(
                enrolledProgram = Program(0, longName, dummyCountryName)
            )
        )
        composeRule.onNodeWithText("Program: $longName").assertIsDisplayed()
    }

    @Test
    fun landUi_e02_clickingTiles_withNoCallback_doesNotCrash() {
        launchLandingScreen(initialState = LandingState(enrolledProgram = Program(0, "", "")))
        clickTile(LandingTestTags.TILE_NEW_SURVEILLANCE)
        clickTile(LandingTestTags.TILE_DATA_COLLECTION)
        clickTile(LandingTestTags.TILE_INCOMPLETE)
        clickTile(LandingTestTags.TILE_COMPLETE)
    }
}
