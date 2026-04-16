package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.vci.vectorcamapp.core.presentation.util.ScreenHeaderTestTags
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.presentation.model.IntakeErrors
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@HiltAndroidTest
class IntakeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // =========================
    // Helpers
    // =========================

    private val noErrors = IntakeErrors(
        collector = null,
        district = null,
        villageName = null,
        houseNumber = null,
        llinType = null,
        llinBrand = null,
        collectionDate = null,
        collectionMethod = null,
        specimenCondition = null,
        monthsSinceIrs = null,
        numLlinsAvailable = null,
        numPeopleSleptUnderLlin = null,
        numPeopleSleptInHouse = null,
        locationTypeSiteSelections = emptyMap(),
        formAnswerErrors = emptyMap(),
    )

    private fun makeSession(
        type: SessionType = SessionType.SURVEILLANCE,
        latitude: Float? = null,
        longitude: Float? = null,
        collectorName: String = "",
        collectorTitle: String = "",
        notes: String = "",
    ) = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        hardwareId = null,
        collectorTitle = collectorTitle,
        collectorName = collectorName,
        collectorLastTrainedOn = 0L,
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        specimenCondition = "",
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        submittedAt = null,
        notes = notes,
        latitude = latitude,
        longitude = longitude,
        type = type,
    )

    private fun scrollToText(text: String) {
        composeRule.onNodeWithTag(ScreenHeaderTestTags.LIST)
            .performScrollToNode(hasText(text, substring = true))
    }

    private fun launchIntakeScreen(
        state: IntakeState = IntakeState(),
        onAction: ((IntakeAction) -> Unit)? = null,
    ) {
        composeRule.activity.setContent {
            VectorcamappTheme {
                androidx.compose.material3.Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IntakeScreen(
                        state = state,
                        onAction = { onAction?.invoke(it) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    // =========================
    // A. Header
    // =========================

    @Test
    fun intakeUi_a01_header_surveillanceSession_showsCorrectTitle() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.SURVEILLANCE))
        )
        composeRule.onNodeWithText("Surveillance Intake").assertIsDisplayed()
        composeRule.onNodeWithText("Please fill out the information below").assertIsDisplayed()
    }

    @Test
    fun intakeUi_a02_header_practiceSession_showsCorrectTitle() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.PRACTICE))
        )
        composeRule.onNodeWithText("Practice Intake").assertIsDisplayed()
        composeRule.onNodeWithText("Please fill out the information below").assertIsDisplayed()
    }

    @Test
    fun intakeUi_a03_header_dataCollectionSession_showsCorrectTitle() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.DATA_COLLECTION))
        )
        composeRule.onNodeWithText("Data Collection Intake").assertIsDisplayed()
    }

    // =========================
    // B. Back navigation
    // =========================

    @Test
    fun intakeUi_b01_backButton_invokesReturnToPreviousScreenAction() {
        var lastAction: IntakeAction? = null
        launchIntakeScreen(
            state = IntakeState(session = makeSession()),
            onAction = { lastAction = it }
        )
        composeRule.onNodeWithContentDescription("Back Button")
            .assertExists()
            .assertHasClickAction()
            .performClick()
        assert(lastAction == IntakeAction.ReturnToPreviousScreen)
    }

    // =========================
    // C. Practice session warning banner
    // =========================

    @Test
    fun intakeUi_c01_practiceWarningBanner_shownForPracticeSession() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.PRACTICE))
        )
        composeRule.onNodeWithText(
            "This is a practice session. Submitted data will not be considered routine surveillance data."
        ).assertIsDisplayed()
    }

    @Test
    fun intakeUi_c02_practiceWarningBanner_notShownForSurveillanceSession() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.SURVEILLANCE))
        )
        composeRule.onNodeWithText(
            "This is a practice session. Submitted data will not be considered routine surveillance data."
        ).assertDoesNotExist()
    }

    // =========================
    // D. Section tile titles
    // =========================

    @Test
    fun intakeUi_d01_sectionTitles_allVisible() {
        launchIntakeScreen(state = IntakeState(session = makeSession()))
        composeRule.onNodeWithText("General Information").assertIsDisplayed()
        scrollToText("Geographical Information")
        composeRule.onNodeWithText("Geographical Information").assertIsDisplayed()
        scrollToText("Additional Notes")
        composeRule.onNodeWithText("Additional Notes").assertIsDisplayed()
    }

    // =========================
    // E. General information fields
    // =========================

    @Test
    fun intakeUi_e01_generalInfoFields_visible() {
        launchIntakeScreen(state = IntakeState(session = makeSession()))
        // "Collector" label appears twice in M3 TextField semantics (label + merged field node)
        composeRule.onAllNodesWithText("Collector")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Hardware ID")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Collection Date")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Collection Method")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Specimen Condition")[0].assertIsDisplayed()
    }

    @Test
    fun intakeUi_e02_collectionMethodTooltipLink_visible() {
        launchIntakeScreen(state = IntakeState(session = makeSession()))
        composeRule.onNodeWithText("Tap to learn more about collection methods")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun intakeUi_e03_collectionMethodTooltipLink_invokesShowTooltipAction() {
        var lastAction: IntakeAction? = null
        launchIntakeScreen(
            state = IntakeState(session = makeSession()),
            onAction = { lastAction = it }
        )
        composeRule.onNodeWithText("Tap to learn more about collection methods").performClick()
        assert(lastAction == IntakeAction.ShowCollectionMethodTooltipDialog)
    }

    // =========================
    // F. Missing collector warning
    // =========================


    @Test
    fun intakeUi_f02_missingCollectorWarning_notShownNormally() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(),
                isCurrentCollectorMissing = false,
            )
        )
        composeRule.onNodeWithText("Collector not found").assertDoesNotExist()
        composeRule.onNodeWithText("Register Missing Collector").assertDoesNotExist()
    }

    @Test
    fun intakeUi_f03_registerMissingCollector_isClickable_andInvokesAction() {
        var lastAction: IntakeAction? = null
        val collector = Collector(
            id = UUID.randomUUID(),
            name = "Jane Doe",
            title = "MSc",
            lastTrainedOn = 0L
        )
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(
                    collectorName = collector.name,
                    collectorTitle = collector.title
                ),
                isCurrentCollectorMissing = true,
            ),
            onAction = { lastAction = it }
        )
        composeRule.onNodeWithText("Register Missing Collector")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assert(lastAction == IntakeAction.RegisterMissingCollector)
    }

    // =========================
    // G. Location states
    // =========================

    @Test
    fun intakeUi_g01_locationLoading_shownWhenNoLatLon() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = null, longitude = null),
                locationError = null,
            )
        )
        scrollToText("Getting location")
        composeRule.onNodeWithText("Getting location…").assertIsDisplayed()
    }

    @Test
    fun intakeUi_g02_locationPills_shownWhenLatLonPresent() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = 1.2345f, longitude = 36.789f),
            )
        )
        scrollToText("Latitude")
        composeRule.onNodeWithText("Latitude: 1.2345").assertIsDisplayed()
        composeRule.onNodeWithText("Longitude: 36.789").assertIsDisplayed()
    }

    @Test
    fun intakeUi_g03_locationError_shownWhenErrorPresent() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = null, longitude = null),
                locationError = IntakeError.LOCATION_PERMISSION_DENIED,
            )
        )
        scrollToText("Could not get location")
        composeRule.onNodeWithText("Could not get location:", substring = true).assertIsDisplayed()
    }

    @Test
    fun intakeUi_g04_retryLocationButton_shownOnlyForGpsTimeout() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = null, longitude = null),
                locationError = IntakeError.LOCATION_GPS_TIMEOUT,
            )
        )
        scrollToText("Retry Location")
        composeRule.onNodeWithText("Retry Location").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun intakeUi_g05_retryLocationButton_notShownForPermissionDenied() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = null, longitude = null),
                locationError = IntakeError.LOCATION_PERMISSION_DENIED,
            )
        )
        composeRule.onNodeWithText("Retry Location").assertDoesNotExist()
    }

    @Test
    fun intakeUi_g06_retryLocation_invokesRetryLocationAction() {
        var lastAction: IntakeAction? = null
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(latitude = null, longitude = null),
                locationError = IntakeError.LOCATION_GPS_TIMEOUT,
            ),
            onAction = { lastAction = it }
        )
        scrollToText("Retry Location")
        composeRule.onNodeWithText("Retry Location").performClick()
        assert(lastAction == IntakeAction.RetryLocation)
    }

    // =========================
    // H. Notes field
    // =========================

    @Test
    fun intakeUi_h01_notesField_visible() {
        launchIntakeScreen(state = IntakeState(session = makeSession()))
        scrollToText("Notes")
        composeRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun intakeUi_h02_notesField_showsExistingNotes() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(notes = "Test note content"))
        )
        scrollToText("Test note content")
        composeRule.onNodeWithText("Test note content").assertIsDisplayed()
    }

    // =========================
    // I. Submit button
    // =========================

    @Test
    fun intakeUi_i01_submitButton_showsCorrectLabel_forSurveillance() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.SURVEILLANCE))
        )
        scrollToText("Begin Surveillance Imaging")
        composeRule.onNodeWithText("Begin Surveillance Imaging").assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun intakeUi_i02_submitButton_showsCorrectLabel_forPractice() {
        launchIntakeScreen(
            state = IntakeState(session = makeSession(type = SessionType.PRACTICE))
        )
        scrollToText("Begin Practice Imaging")
        composeRule.onNodeWithText("Begin Practice Imaging").assertIsDisplayed()
    }

    @Test
    fun intakeUi_i03_submitButton_invokesSubmitIntakeFormAction() {
        var lastAction: IntakeAction? = null
        launchIntakeScreen(
            state = IntakeState(session = makeSession()),
            onAction = { lastAction = it }
        )
        scrollToText("Begin Surveillance Imaging")
        composeRule.onNodeWithText("Begin Surveillance Imaging").performClick()
        assert(lastAction == IntakeAction.SubmitIntakeForm)
    }

    // =========================
    // J. Legacy Surveillance Form tile
    // =========================

    @Test
    fun intakeUi_j01_surveillanceFormTile_shownWhenSurveillanceFormPresent() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(),
                form = null,
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = -1,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = -1,
                    llinType = null,
                    llinBrand = null,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null,
                )
            )
        )
        scrollToText("Surveillance Form")
        composeRule.onNodeWithText("Surveillance Form").assertIsDisplayed()
        scrollToText("Number of People Living in the House")
        composeRule.onNodeWithText("Number of People Living in the House").assertIsDisplayed()
        scrollToText("Was IRS conducted in this household?")
        composeRule.onNodeWithText("Was IRS conducted in this household?").assertIsDisplayed()
    }

    @Test
    fun intakeUi_j02_surveillanceFormTile_notShownWhenBothFormAndSurveillanceFormNull() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(),
                form = null,
                surveillanceForm = null,
            )
        )
        composeRule.onNodeWithText("Surveillance Form").assertDoesNotExist()
        composeRule.onNodeWithText("Number of People Living in the House").assertDoesNotExist()
    }

    @Test
    fun intakeUi_j03_monthsSinceIrs_shownWhenWasIrsConductedTrue() {
        launchIntakeScreen(
            state = IntakeState(
                session = makeSession(),
                form = null,
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 4,
                    wasIrsConducted = true,
                    monthsSinceIrs = 3,
                    numLlinsAvailable = -1,
                    llinType = null,
                    llinBrand = null,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null,
                )
            )
        )
        scrollToText("Months Since IRS")
        composeRule.onNodeWithText("Months Since IRS").assertIsDisplayed()
    }
}
