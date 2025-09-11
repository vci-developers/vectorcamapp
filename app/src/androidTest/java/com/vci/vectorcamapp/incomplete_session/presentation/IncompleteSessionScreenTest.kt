package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.incomplete_session.presentation.util.IncompleteSessionTestTags
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

@HiltAndroidTest
class IncompleteSessionScreenTest {

    private lateinit var navController: TestNavHostController

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private object TestDestinations {
        const val Incomplete = "incomplete"
        const val Landing = "landing"
        const val Intake = "intake/{type}"

        fun intakeRouteFor(type: SessionType) = "intake/${type.name}"
    }

    private fun sampleSessions(): List<Session> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        fun session(
            type: SessionType,
            createdAt: Long
        ) = Session(
            localId = UUID.randomUUID(),
            remoteId = null,
            houseNumber = "",
            collectorTitle = "",
            collectorName = "",
            collectionDate = createdAt,
            collectionMethod = "",
            specimenCondition = "",
            createdAt = createdAt,
            completedAt = null,
            submittedAt = null,
            notes = "",
            latitude = null,
            longitude = null,
            type = type
        )

        return listOf(
            session(SessionType.SURVEILLANCE, now - 3 * day),
            session(SessionType.DATA_COLLECTION, now - 2 * day),
            session(SessionType.SURVEILLANCE, now - 1 * day),
        )
    }

    private fun launchIncompleteSessionScreen(
        initialState: IncompleteSessionState = IncompleteSessionState()
    ) {
        composeRule.activity.setContent {
            var state by remember { mutableStateOf(initialState) }

            VectorcamappTheme {
                NavHost(
                    navController = navController,
                    startDestination = TestDestinations.Incomplete
                ) {
                    composable(TestDestinations.Incomplete) {
                        BaseScaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            IncompleteSessionScreen(
                                state = state,
                                onAction = { action ->
                                    when (action) {
                                        is IncompleteSessionAction.ResumeSession -> {
                                            val selected = state.sessions.firstOrNull { it.localId == action.sessionId }
                                            val type = selected?.type ?: SessionType.SURVEILLANCE
                                            navController.navigate(TestDestinations.intakeRouteFor(type))
                                        }
                                        is IncompleteSessionAction.ReturnToLandingScreen -> {
                                            navController.navigate(TestDestinations.Landing)
                                        }
                                        is IncompleteSessionAction.DeleteSession -> {
                                            state = state.copy(deleteDialogSessionId = action.sessionId)
                                        }
                                        is IncompleteSessionAction.ConfirmDeleteSession -> {
                                            state = state.copy(
                                                sessions = state.sessions.filterNot { it.localId == action.sessionId },
                                                deleteDialogSessionId = null
                                            )
                                        }
                                        IncompleteSessionAction.DismissDeleteDialog -> {
                                            state = state.copy(deleteDialogSessionId = null)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                    composable(TestDestinations.Landing) { }
                    composable(TestDestinations.Intake) { }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertOnIncomplete() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(TestDestinations.Incomplete)
    }

    private fun assertOnLanding() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(TestDestinations.Landing)
    }

    private fun assertOnIntakeFor(type: SessionType) {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(TestDestinations.Intake)
        val lastArg = navController.currentBackStackEntry?.arguments?.getString("type")
        assertThat(lastArg).isEqualTo(type.name)
    }

    private fun clickableDescendantUnderCard(index: Int) =
        composeRule.onNode(
            hasClickAction()
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-$index")))
                .and(hasAnyDescendant(hasContentDescription("Resume"))),
            useUnmergedTree = true
        )

    private fun swipeLeftToRevealDelete(index: Int) {
        composeRule.onNodeWithTag("${IncompleteSessionTestTags.CARD_PREFIX}-$index", useUnmergedTree = true)
            .performTouchInput {
                val startX = this.visibleSize.width * 0.9f
                val endX = this.visibleSize.width * 0.1f
                val y = this.visibleSize.height / 2f

                swipe(
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(endX, y),
                    durationMillis = 300
                )
            }
        composeRule.waitForIdle()
    }

    // ========================================
    // A. Static UI
    // ========================================

    @Test
    fun incUi_a01_headersAndBackVisibleOnInitialRender() {
        launchIncompleteSessionScreen()
        composeRule.onNodeWithText("Incomplete Sessions").assertIsDisplayed()
        composeRule.onNodeWithText("Click on a session to resume").assertIsDisplayed()
        composeRule.onNodeWithTag(IncompleteSessionTestTags.BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back Button").assertIsDisplayed()
    }

    // ========================================
    // B. List Rendering / Content
    // ========================================

    @Test
    fun incUi_b01_noSessions_showsNoCards() {
        launchIncompleteSessionScreen()
        composeRule.onAllNodesWithTag("${IncompleteSessionTestTags.CARD_PREFIX}-0")
            .assertCountEquals(0)
    }

    @Test
    fun incUi_b02_sessionsPresent_cardsRenderWithTagsAndTexts() {
        val sessions = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))

        composeRule.onNodeWithTag("${IncompleteSessionTestTags.CARD_PREFIX}-0").assertIsDisplayed()
        composeRule.onNodeWithTag("${IncompleteSessionTestTags.CARD_PREFIX}-1").assertIsDisplayed()
        composeRule.onNodeWithTag("${IncompleteSessionTestTags.CARD_PREFIX}-2").assertIsDisplayed()

        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_TITLE, useUnmergedTree = true)
            .assertCountEquals(sessions.size)
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_TYPE_PILL, useUnmergedTree = true)
            .assertCountEquals(sessions.size)
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_CREATED_TEXT, useUnmergedTree = true)
            .assertCountEquals(sessions.size)
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_UPDATED_TEXT, useUnmergedTree = true)
            .assertCountEquals(sessions.size)
    }

    @Test
    fun incUi_b03_resumeIconVisibleOnEachCard() {
        val sessions = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_RESUME_ICON, useUnmergedTree = true)
            .assertCountEquals(sessions.size)
    }

    @Test
    fun incUi_b04_cardContentIsCorrect() {
        val sessions = sampleSessions()
        val middleSession = sessions[1]
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))

        val cardMatcher = hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-1")
        val titleFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val expectedTitle = "Incomplete Session on ${titleFormatter.format(middleSession.createdAt)}"
        val expectedPillText = "Session Type: ${middleSession.type.name}"

        // Assert title text is correct and within the correct card
        composeRule.onNode(
            hasText(expectedTitle)
                .and(hasTestTag(IncompleteSessionTestTags.CARD_TITLE))
                .and(hasAnyAncestor(cardMatcher)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // Assert pill text is correct and within the correct card
        composeRule.onNode(
            hasText(expectedPillText)
                .and(hasAnyAncestor(hasTestTag(IncompleteSessionTestTags.CARD_TYPE_PILL)))
                .and(hasAnyAncestor(cardMatcher)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun incUi_b05_createdAndUpdatedTextContentAreCorrect() {
        val sessionList = sampleSessions()
        val middleSession = sessionList[1]
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessionList))

        val detailFormatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        val expected = "Created: ${detailFormatter.format(middleSession.createdAt)}"

        composeRule.onNode(
            hasText(expected).and(
                hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-1"))
            ),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // TODO: Implement when we implement update time
        // composeRule.onNode(
        //      hasText("Last Updated: ...").and(
        //           hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-1"))
        //      ),
        //      useUnmergedTree = true
        // ).assertIsDisplayed()
    }

    // ========================================
    // C. Interactions - Navigation
    // ========================================

    @Test
    fun incUi_c01_backNavigatesToLanding() {
        launchIncompleteSessionScreen(IncompleteSessionState(sampleSessions()))
        assertOnIncomplete()
        composeRule.onNodeWithTag(IncompleteSessionTestTags.BACK_BUTTON)
            .assertHasClickAction()
            .performClick()
        assertOnLanding()
    }

    @Test
    fun incUi_c02_clickingNewestCardNavigatesToIntakeWithCorrectType() {
        val sessions = sampleSessions()
        val expectedType = sessions.last().type // The list is reversed in the UI
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))

        clickableDescendantUnderCard(0).performClick()
        assertOnIntakeFor(expectedType)
    }

    @Test
    fun incUi_c03_clickingMiddleCardNavigatesWithCorrectType() {
        val sessions = sampleSessions()
        val expectedType = sessions[1].type
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))

        clickableDescendantUnderCard(1).performClick()
        assertOnIntakeFor(expectedType)
    }

    // ========================================
    // D. Accessibility Semantics
    // ========================================

    @Test
    fun incUi_d01_contentDescriptionsPresent() {
        val sessions = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))
        composeRule.onNodeWithContentDescription("Back Button").assertIsDisplayed()
        val resumeIcons = composeRule.onAllNodesWithContentDescription("Resume", useUnmergedTree = true)
        resumeIcons.assertCountEquals(sessions.size)
    }

    // ========================================
    // E. Regression / Edge Cases
    // ========================================

    @Test
    fun incUi_e01_cardsAreClickableWhenPresent() {
        val sessions = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessions))
        clickableDescendantUnderCard(1).assertHasClickAction()
    }

    // ========================================
    // F. Session Deletion Workflow
    // ========================================

    @Test
    fun incUi_f01_tappingDeleteIconShowsDialog() {
        val sessionList = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessionList))

        swipeLeftToRevealDelete(index = 0)

        composeRule.onNode(
            hasContentDescription("Delete")
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-0"))),
            useUnmergedTree = true
        ).onParent().performClick()

        composeRule.onNodeWithText("Delete Session?").assertIsDisplayed()
        composeRule.onNodeWithText("Yes, Delete", ignoreCase = true).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Cancel", ignoreCase = true).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun incUi_f02_cancelDismissesDialogAndKeepsList() {
        val sessionList = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessionList))

        swipeLeftToRevealDelete(index = 0)
        composeRule.onNode(
            hasContentDescription("Delete")
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-0"))),
            useUnmergedTree = true
        ).onParent().performClick()

        composeRule.onNodeWithText("Cancel", ignoreCase = true).performClick()

        composeRule.onNodeWithText("Delete Session?").assertDoesNotExist()
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_TITLE, useUnmergedTree = true)
            .assertCountEquals(sessionList.size)
    }

    @Test
    fun incUi_f03_confirmDeletesCardAndHidesDialog() {
        val sessionList = sampleSessions()
        launchIncompleteSessionScreen(IncompleteSessionState(sessions = sessionList))

        swipeLeftToRevealDelete(index = 0)
        composeRule.onNode(
            hasContentDescription("Delete")
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-0"))),
            useUnmergedTree = true
        ).onParent().performClick()

        composeRule.onNodeWithText("Yes, Delete", ignoreCase = true).performClick()

        composeRule.onNodeWithText("Delete Session?").assertDoesNotExist()
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_TITLE, useUnmergedTree = true)
            .assertCountEquals(sessionList.size - 1)
    }
}