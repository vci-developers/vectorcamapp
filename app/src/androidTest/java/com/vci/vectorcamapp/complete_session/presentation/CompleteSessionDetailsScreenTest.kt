package com.vci.vectorcamapp.complete_session.presentation

import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsAction
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsScreen
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsState
import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
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
class CompleteSessionDetailsScreenTest {

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
        const val Details = "details"
        const val List = "list"
    }

    private fun makeSession(): Session {
        val now = System.currentTimeMillis()
        return Session(
            localId = UUID.randomUUID(),
            remoteId = 101,
            hardwareId = "TEST-HW-123",
            collectorTitle = "Dr.",
            collectorName = "Jane Doe",
            collectorLastTrainedOn = now,
            collectionDate = now,
            collectionMethod = "HLC",
            specimenCondition = "Fresh",
            createdAt = now,
            completedAt = now + 3600000,
            submittedAt = null,
            notes = "Test notes for session.",
            latitude = 0.0f,
            longitude = 0.0f,
            type = SessionType.SURVEILLANCE
        )
    }

    private fun makeSite(): Site {
        return Site(
            id = 1,
            district = "Test District",
            subCounty = "Test SubCounty",
            parish = "Test Parish",
            villageName = "Test Village",
            houseNumber = "H-101",
            healthCenter = "Test HC",
            isActive = true
        )
    }

    private fun makeSurveillanceForm(): SurveillanceForm {
        return SurveillanceForm(
            numPeopleSleptInHouse = 5,
            wasIrsConducted = true,
            monthsSinceIrs = 3,
            numLlinsAvailable = 2,
            llinType = "Test Type",
            llinBrand = "Test Brand",
            numPeopleSleptUnderLlin = 4,
            submittedAt = System.currentTimeMillis()
        )
    }

    private fun makeSpecimens(count: Int): List<SpecimenWithSpecimenImagesAndInferenceResults> {
        return (1..count).map { index ->
            val specimenId = "SPEC-$index"
            val specimen = Specimen(
                id = specimenId,
                remoteId = index,
                shouldProcessFurther = true
            )
            val image = SpecimenImage(
                localId = UUID.randomUUID().toString(),
                remoteId = index,
                species = "Anopheles gambiae",
                sex = "Female",
                abdomenStatus = "Fed",
                imageUri = Uri.parse("file://test/uri/$index"),
                metadataUploadStatus = UploadStatus.COMPLETED,
                imageUploadStatus = UploadStatus.COMPLETED,
                capturedAt = System.currentTimeMillis(),
                submittedAt = System.currentTimeMillis()
            )
            SpecimenWithSpecimenImagesAndInferenceResults(
                specimen = specimen,
                specimenImagesAndInferenceResults = listOf(
                    SpecimenImageAndInferenceResult(
                        specimenImage = image,
                        inferenceResult = null
                    )
                )
            )
        }
    }

    private fun launchCompleteSessionDetailsScreen(
        initialState: CompleteSessionDetailsState
    ) {
        composeRule.activity.setContent {
            var state by remember { mutableStateOf(initialState) }

            VectorcamappTheme {
                NavHost(
                    navController = navController,
                    startDestination = TestDestinations.Details
                ) {
                    composable(TestDestinations.Details) {
                        BaseScaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            CompleteSessionDetailsScreen(
                                state = state,
                                onAction = { action ->
                                    when (action) {
                                        is CompleteSessionDetailsAction.ReturnToCompleteSessionListScreen -> {
                                            navController.navigate(TestDestinations.List)
                                        }

                                        is CompleteSessionDetailsAction.ChangeSelectedTab -> {
                                            state = state.copy(selectedTab = action.selectedTab)
                                        }

                                        is CompleteSessionDetailsAction.UpdateSearchQuery -> {
                                            state = state.copy(searchQuery = action.searchQuery)
                                        }

                                        CompleteSessionDetailsAction.ShowSearchTooltipDialog -> {
                                            state = state.copy(isSearchTooltipVisible = true)
                                        }

                                        CompleteSessionDetailsAction.HideSearchTooltipDialog -> {
                                            state = state.copy(isSearchTooltipVisible = false)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                    composable(TestDestinations.List) { }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertOnDetails() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(TestDestinations.Details)
    }

    private fun assertOnList() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(TestDestinations.List)
    }

    // ========================================
    // A. Static UI & Header
    // ========================================

    @Test
    fun cplUi_a01_headersAndBackVisibleOnInitialRender() {
        val session = makeSession()
        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = makeSite()
            )
        )

        composeRule.onNodeWithText("Session Information").assertIsDisplayed()
        composeRule.onNodeWithText("ID: ${session.localId}").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back Button").assertIsDisplayed()

        composeRule.onNodeWithText(CompleteSessionDetailsTab.SESSION_FORM.label).assertIsDisplayed()
        composeRule.onNodeWithText(CompleteSessionDetailsTab.SESSION_SPECIMENS.label).assertIsDisplayed()
    }

    // ========================================
    // B. Form Tab (Default) Rendering
    // ========================================

    @Test
    fun cplUi_b01_formTab_showsSessionStatusAndGeneralInfo() {
        val session = makeSession()
        val site = makeSite()
        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                selectedTab = CompleteSessionDetailsTab.SESSION_FORM
            )
        )

        composeRule.onNodeWithText("Session Status", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        composeRule.onNodeWithText("Collection Date: ${dateFormat.format(session.collectionDate)}", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("General Information", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Collector: ${session.collectorName}, ${session.collectorTitle}", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cplUi_b02_formTab_showsGeographicalAndSurveillanceInfo() {
        val session = makeSession()
        val site = makeSite()
        val surveillance = makeSurveillanceForm()

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                surveillanceForm = surveillance,
                selectedTab = CompleteSessionDetailsTab.SESSION_FORM
            )
        )

        composeRule.onNodeWithText("Geographical Information", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("District: ${site.district}", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Village Name: ${site.villageName}", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Surveillance Form", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Number of People who Slept in the House: ${surveillance.numPeopleSleptInHouse}", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // ========================================
    // C. Tab Interactions
    // ========================================

    @Test
    fun cplUi_c01_switchingTabsChangesContent() {
        val session = makeSession()
        val site = makeSite()

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                selectedTab = CompleteSessionDetailsTab.SESSION_FORM
            )
        )

        composeRule.onNodeWithText("Geographical Information", useUnmergedTree = true).assertExists()

        composeRule.onNodeWithText(CompleteSessionDetailsTab.SESSION_SPECIMENS.label).performClick()

        composeRule.onNodeWithText("Geographical Information").assertDoesNotExist()

        composeRule.onNodeWithText("No specimens were captured during this session.").assertIsDisplayed()
    }

    // ========================================
    // D. Specimens Tab Rendering
    // ========================================

    @Test
    fun cplUi_d01_specimensTab_emptyState() {
        val session = makeSession()
        val site = makeSite()

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                specimensWithImagesAndInferenceResults = emptyList(),
                selectedTab = CompleteSessionDetailsTab.SESSION_SPECIMENS
            )
        )

        composeRule.onNodeWithText("No specimens were captured during this session.")
            .assertIsDisplayed()
    }

    @Test
    fun cplUi_d02_specimensTab_showsSpecimenTiles() {
        val session = makeSession()
        val site = makeSite()
        val specimens = makeSpecimens(3)

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                specimensWithImagesAndInferenceResults = specimens,
                selectedTab = CompleteSessionDetailsTab.SESSION_SPECIMENS
            )
        )

        val newestSpecimenId = specimens.last().specimen.id

        composeRule.onNodeWithText("Specimen ID: $newestSpecimenId", useUnmergedTree = true)
            .performScrollTo()
            .assertExists()

        val species = specimens.last().specimenImagesAndInferenceResults.first().specimenImage.species
        composeRule.onAllNodesWithText("Species: $species", useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun cplUi_d03_specimenBadgeShowsCorrectCount() {
        val session = makeSession()
        val specimens = makeSpecimens(1)

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = makeSite(),
                specimensWithImagesAndInferenceResults = specimens,
                selectedTab = CompleteSessionDetailsTab.SESSION_SPECIMENS
            )
        )

        composeRule.onNodeWithText("1 of 1", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ========================================
    // E. Interactions - Search
    // ========================================

    @Test
    fun cplUi_e01_searchBarInputUpdatesState() {
        val session = makeSession()
        val site = makeSite()
        val specimens = makeSpecimens(1)

        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = site,
                specimensWithImagesAndInferenceResults = specimens,
                selectedTab = CompleteSessionDetailsTab.SESSION_SPECIMENS
            )
        )

        val searchText = "SPEC-1"
        composeRule.onNodeWithText("Search by specimen ID, species, etc.")
            .performTextInput(searchText)

        composeRule.onNodeWithText(searchText).assertIsDisplayed()
    }

    // ========================================
    // F. Interactions - Navigation
    // ========================================

    @Test
    fun cplUi_f01_backNavigatesToList() {
        val session = makeSession()
        launchCompleteSessionDetailsScreen(
            CompleteSessionDetailsState(
                session = session,
                site = makeSite()
            )
        )

        assertOnDetails()
        composeRule.onNodeWithContentDescription("Back Button")
            .assertHasClickAction()
            .performClick()
        assertOnList()
    }
}
