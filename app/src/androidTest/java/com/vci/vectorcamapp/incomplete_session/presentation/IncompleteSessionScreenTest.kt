package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
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

    private fun makeSessionAndSite(session: Session, siteId: Int): SessionAndSite {
        val site = Site(
            id = siteId,
            district = "District $siteId",
            subCounty = "Sub-County $siteId",
            parish = "Parish $siteId",
            villageName = "Village $siteId",
            houseNumber = "House #$siteId",
            healthCenter = "Health Center #$siteId",
            isActive = true
        )
        return SessionAndSite(session = session, site = site)
    }

    private fun sampleSessionAndSites(): List<SessionAndSite> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        fun makeSession(
            type: SessionType,
            createdAt: Long
        ) = Session(
            localId = UUID.randomUUID(),
            remoteId = null,
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
            makeSessionAndSite(makeSession(SessionType.SURVEILLANCE, now - 3 * day), 0),
            makeSessionAndSite(makeSession(SessionType.DATA_COLLECTION, now - 2 * day), 1),
            makeSessionAndSite(makeSession(SessionType.SURVEILLANCE, now - 1 * day), 2)
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
                                            val selected = state.sessionAndSites.firstOrNull { it.session.localId == action.sessionId }
                                            val type = selected?.session?.type ?: SessionType.SURVEILLANCE
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
                                                sessionAndSites = state.sessionAndSites.filterNot { it.session.localId == action.sessionId },
                                                deleteDialogSessionId = null
                                            )
                                        }
                                        IncompleteSessionAction.DismissDeleteDialog -> {
                                            state = state.copy(deleteDialogSessionId = null)
                                        }

                                        is IncompleteSessionAction.UpdateSearchQuery -> TODO()
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
                    start = Offset(startX, y),
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
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        val cardIndices = sessionAndSites.indices

        cardIndices.forEach { index ->
            val cardTestTag = "${IncompleteSessionTestTags.CARD_PREFIX}-$index"

            composeRule.onNodeWithTag(cardTestTag, useUnmergedTree = true)
                .performScrollTo()
                .assertExists()

            composeRule.onNode(
                hasTestTag(IncompleteSessionTestTags.CARD_TITLE)
                    .and(hasAnyAncestor(hasTestTag(cardTestTag))),
                useUnmergedTree = true
            ).assertExists()

            composeRule.onNode(
                hasTestTag(IncompleteSessionTestTags.CARD_TYPE_PILL)
                    .and(hasAnyAncestor(hasTestTag(cardTestTag))),
                useUnmergedTree = true
            ).assertExists()

            composeRule.onNode(
                hasTestTag(IncompleteSessionTestTags.CARD_CREATED_TEXT)
                    .and(hasAnyAncestor(hasTestTag(cardTestTag))),
                useUnmergedTree = true
            ).assertExists()
        }
    }

    @Test
    fun incUi_b03_resumeIconVisibleOnEachCard() {
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        sessionAndSites.indices.forEach { index ->
            val cardTestId = "${IncompleteSessionTestTags.CARD_PREFIX}-$index"

            composeRule.onNodeWithTag(cardTestId, useUnmergedTree = true)
                .performScrollTo()
                .assertExists()

            composeRule.onNode(
                hasTestTag(IncompleteSessionTestTags.CARD_RESUME_ICON)
                    .and(hasAnyAncestor(hasTestTag(cardTestId))),
                useUnmergedTree = true
            ).assertExists()
        }
    }

    @Test
    fun incUi_b04_cardContentIsCorrect() {
        val sessionAndSites = sampleSessionAndSites()
        val middleSessionAndSite = sessionAndSites[1]
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        val cardMatcher = hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-1")
        val titleFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val expectedTitle = "Incomplete Session on\n${titleFormatter.format(middleSessionAndSite.session.createdAt)}"
        val expectedPillText = "Session Type: ${middleSessionAndSite.session.type.name}"

        composeRule.onNode(cardMatcher, useUnmergedTree = true)
            .performScrollTo()
            .assertExists()

        composeRule.onNode(
            hasText(expectedTitle)
                .and(hasTestTag(IncompleteSessionTestTags.CARD_TITLE))
                .and(hasAnyAncestor(cardMatcher)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        composeRule.onNode(
            hasText(expectedPillText)
                .and(hasAnyAncestor(cardMatcher))
                .and(hasAnyAncestor(hasTestTag(IncompleteSessionTestTags.CARD_TYPE_PILL))),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun incUi_b05_createdAndUpdatedTextContentAreCorrect() {
        val sessionAndSites = sampleSessionAndSites()
        val middleSessionAndSite = sessionAndSites[1]
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        val detailFormatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        val expectedText = "Created At: ${detailFormatter.format(middleSessionAndSite.session.createdAt)}"
        val cardTestId = "${IncompleteSessionTestTags.CARD_PREFIX}-1"

        composeRule.onNodeWithTag(cardTestId, useUnmergedTree = true)
            .performScrollTo()
            .assertExists()

        composeRule.onNode(
            hasText(expectedText)
                .and(hasAnyAncestor(hasTestTag(cardTestId))),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    // ========================================
    // C. Interactions - Navigation
    // ========================================

    @Test
    fun incUi_c01_backNavigatesToLanding() {
        launchIncompleteSessionScreen(IncompleteSessionState(sampleSessionAndSites()))
        assertOnIncomplete()
        composeRule.onNodeWithTag(IncompleteSessionTestTags.BACK_BUTTON)
            .assertHasClickAction()
            .performClick()
        assertOnLanding()
    }

    @Test
    fun incUi_c02_clickingNewestCardNavigatesToIntakeWithCorrectType() {
        val sessionAndSites = sampleSessionAndSites()
        val expectedType = sessionAndSites.last().session.type
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        clickableDescendantUnderCard(0).performClick()
        assertOnIntakeFor(expectedType)
    }

    @Test
    fun incUi_c03_clickingMiddleCardNavigatesWithCorrectType() {
        val sessionAndSites = sampleSessionAndSites()
        val expectedType = sessionAndSites[1].session.type
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        clickableDescendantUnderCard(1).performClick()
        assertOnIntakeFor(expectedType)
    }

    // ========================================
    // D. Accessibility Semantics
    // ========================================

    @Test
    fun incUi_d01_contentDescriptionsPresent() {
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))
        composeRule.onNodeWithContentDescription("Back Button").assertIsDisplayed()
        sessionAndSites.indices.forEach { index ->
            val cardTestTag = "${IncompleteSessionTestTags.CARD_PREFIX}-$index"

            composeRule.onNodeWithTag(cardTestTag, useUnmergedTree = true)
                .performScrollTo()

            composeRule.onNode(
                hasContentDescription("Resume")
                    .and(hasAnyAncestor(hasTestTag(cardTestTag))),
                useUnmergedTree = true
            ).assertExists()
        }
    }

    // ========================================
    // E. Regression / Edge Cases
    // ========================================

    @Test
    fun incUi_e01_cardsAreClickableWhenPresent() {
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))
        clickableDescendantUnderCard(1).assertHasClickAction()
    }

    // ========================================
    // F. Session Deletion Workflow
    // ========================================

    @Test
    fun incUi_f01_tappingDeleteIconShowsDialog() {
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

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
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        swipeLeftToRevealDelete(index = 0)
        composeRule.onNode(
            hasContentDescription("Delete")
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-0"))),
            useUnmergedTree = true
        ).onParent().performClick()

        composeRule.onNodeWithText("Cancel", ignoreCase = true).performClick()

        composeRule.onNodeWithText("Delete Session?").assertDoesNotExist()

        val lastIndex = sessionAndSites.lastIndex
        for (index in 0..lastIndex) {
            val cardTag = "${IncompleteSessionTestTags.CARD_PREFIX}-$index"

            composeRule
                .onNodeWithTag(cardTag, useUnmergedTree = true)
                .performScrollTo()
                .assertExists()

            composeRule
                .onNode(
                    hasTestTag(IncompleteSessionTestTags.CARD_TITLE)
                        .and(hasAnyAncestor(hasTestTag(cardTag))),
                    useUnmergedTree = true
                )
                .assertExists()
        }
    }

    @Test
    fun incUi_f03_confirmDeletesCardAndHidesDialog() {
        val sessionAndSites = sampleSessionAndSites()
        launchIncompleteSessionScreen(IncompleteSessionState(sessionAndSites = sessionAndSites))

        swipeLeftToRevealDelete(index = 0)
        composeRule.onNode(
            hasContentDescription("Delete")
                .and(hasAnyAncestor(hasTestTag("${IncompleteSessionTestTags.CARD_PREFIX}-0"))),
            useUnmergedTree = true
        ).onParent().performClick()

        composeRule.onNodeWithText("Yes, Delete", ignoreCase = true).performClick()

        composeRule.onNodeWithText("Delete Session?").assertDoesNotExist()
        composeRule.onAllNodesWithTag(IncompleteSessionTestTags.CARD_TITLE, useUnmergedTree = true)
            .assertCountEquals(sessionAndSites.size - 1)
    }
}