package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.presentation.util.IntakeTestTags
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class IntakeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }

    // -----------------------------
    // Test harness
    // -----------------------------
    private fun launchIntake(initial: IntakeState = IntakeState()) {
        composeRule.activity.setContent {
            var state = initial
            VectorcamappTheme {
                NavHost(navController = navController, startDestination = "intake") {
                    composable("intake") {
                        BaseScaffold(modifier = Modifier.fillMaxSize()) { inner ->
                            IntakeScreen(
                                state = state,
                                onAction = { action ->
                                    when (action) {
                                        is IntakeAction.ReturnToLandingScreen -> navController.navigate("landing")
                                        else -> Unit
                                    }
                                },
                                modifier = Modifier.padding(inner)
                            )
                        }
                    }
                    composable("landing") { }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertOnIntake() {
        assertThat(navController.currentDestination?.route).isEqualTo("intake")
    }

    private fun assertOnLanding() {
        assertThat(navController.currentDestination?.route).isEqualTo("landing")
    }

    // =========================================================================
    // A. Static UI & basic elements
    // =========================================================================

    @Test
    fun intake_a01_headers_tiles_and_controls_render() {
        launchIntake()

        // Header container
        composeRule.onNodeWithTag(IntakeTestTags.INTAKE_SCREEN).assertIsDisplayed()

        // General Information tile
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.TILE_GENERAL))
        composeRule.onNodeWithTag(IntakeTestTags.TILE_GENERAL, useUnmergedTree = true)
            .assertExists()

        // Geographical Information tile
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.DISTRICT_DD))
        composeRule.onNodeWithTag(IntakeTestTags.DISTRICT_DD, useUnmergedTree = true)
            .assertExists()
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.TILE_GEOGRAPHICAL))
        composeRule.onNodeWithTag(IntakeTestTags.TILE_GEOGRAPHICAL).assertExists()

        // Notes tile
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.NOTES))
        composeRule.onNodeWithTag(IntakeTestTags.NOTES, useUnmergedTree = true)
            .assertExists()
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.TILE_NOTES))
        composeRule.onNodeWithTag(IntakeTestTags.TILE_NOTES).assertExists()

        // Continue button
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.CONTINUE_BUTTON))
        composeRule.onNodeWithTag(IntakeTestTags.CONTINUE_BUTTON, useUnmergedTree = true)
            .assertExists()

        composeRule.onNodeWithTag(IntakeTestTags.TILE_GENERAL)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun intake_a02_core_fields_present() {
        launchIntake()

        composeRule.onNodeWithTag(IntakeTestTags.COLLECTOR_NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.COLLECTOR_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.COLLECTION_DATE).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.COLLECTION_METHOD_DD).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.SPECIMEN_CONDITION_DD).assertIsDisplayed()

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.DISTRICT_DD))
        composeRule.onNodeWithTag(IntakeTestTags.DISTRICT_DD, useUnmergedTree = true).assertExists()

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.HOUSE_NUMBER))
        composeRule.onNodeWithTag(IntakeTestTags.HOUSE_NUMBER, useUnmergedTree = true).assertExists()

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.NOTES))
        composeRule.onNodeWithTag(IntakeTestTags.NOTES, useUnmergedTree = true).assertExists()
    }

    // =========================================================================
    // B. Location state machine: loading / coords / error
    // =========================================================================

    @Test
    fun intake_b01_location_loading_when_no_latlon_and_no_error() {
        launchIntake(initial = IntakeState())

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.LOCATION_LOADING_ROW))

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_LOADING_ROW, useUnmergedTree = true)
            .assertExists()

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_COORDS_ROW).assertDoesNotExist()
        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_ERROR_TEXT).assertDoesNotExist()
    }

    @Test
    fun intake_b02_location_coordinates_when_latitude_longitude_present() {
        val baseState = IntakeState()
        val baseStateWithCoordinates = baseState.copy(
            session = baseState.session.copy(latitude = 11.111f, longitude = 11.111f),
            locationError = null
        )
        launchIntake(baseStateWithCoordinates)

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.LOCATION_COORDS_ROW))

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_COORDS_ROW, useUnmergedTree = true)
            .assertExists()

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_LOADING_ROW).assertDoesNotExist()
        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_ERROR_TEXT).assertDoesNotExist()
    }

    @Test
    fun intake_b03_location_error_timeout_shows_retry() {
        val base = IntakeState()
        val withError = base.copy(
            session = base.session.copy(latitude = null, longitude = null),
            locationError = IntakeError.LOCATION_GPS_TIMEOUT
        )
        launchIntake(withError)

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.LOCATION_ERROR_TEXT))

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_ERROR_TEXT, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_RETRY_BUTTON).assertIsDisplayed()

        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_LOADING_ROW).assertDoesNotExist()
        composeRule.onNodeWithTag(IntakeTestTags.LOCATION_COORDS_ROW).assertDoesNotExist()
    }

    // =========================================================================
    // C. Dropdown rendering/open (labels come from enums)
    // =========================================================================

    @Test
    fun intake_c01_open_collection_method_dropdown_shows_options() {
        launchIntake()
        composeRule.onNodeWithTag(IntakeTestTags.COLLECTION_METHOD_DD).assertIsDisplayed()

        composeRule.onNode(
            hasClickAction() and hasAnyAncestor(hasTestTag(IntakeTestTags.COLLECTION_METHOD_DD)),
            useUnmergedTree = true
        ).performClick()

        val firstLabel = IntakeDropdownOptions.CollectionMethodOption.entries.first().label
        composeRule.onNodeWithText(firstLabel).assertIsDisplayed()
    }

    @Test
    fun intake_c02_open_specimen_condition_dropdown_shows_options() {
        launchIntake()
        composeRule.onNodeWithTag(IntakeTestTags.SPECIMEN_CONDITION_DD).assertIsDisplayed()

        composeRule.onNode(
            hasClickAction() and hasAnyAncestor(hasTestTag(IntakeTestTags.SPECIMEN_CONDITION_DD)),
            useUnmergedTree = true
        ).performClick()

        val firstLabel = IntakeDropdownOptions.SpecimenConditionOption.entries.first().label
        composeRule.onNodeWithText(firstLabel).assertIsDisplayed()
    }

    // =========================================================================
    // D. District / Sentinel visibility
    // =========================================================================

    @Test
    fun intake_d01_sentinel_hidden_when_district_blank() {
        launchIntake(initial = IntakeState(selectedDistrict = ""))

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.DISTRICT_DD))
        composeRule.onNodeWithTag(IntakeTestTags.DISTRICT_DD, useUnmergedTree = true).assertExists()

        composeRule.onNodeWithTag(IntakeTestTags.SENTINEL_SITE_DD).assertDoesNotExist()
    }

    @Test
    fun intake_d02_sentinel_visible_when_district_selected() {
        launchIntake(
            initial = IntakeState(
                selectedDistrict = "District A"
            )
        )

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.SENTINEL_SITE_DD))

        composeRule.onNodeWithTag(IntakeTestTags.SENTINEL_SITE_DD, useUnmergedTree = true)
            .assertExists()
    }

    // =========================================================================
    // E. Surveillance form conditional content
    // =========================================================================

    @Test
    fun intake_e01_surveillance_tile_hidden_when_form_null() {
        val baseState = IntakeState()
        val missingFormState = baseState.copy(surveillanceForm = null)
        launchIntake(missingFormState)
        composeRule.onNodeWithTag(IntakeTestTags.TILE_FORM).assertDoesNotExist()
        composeRule.onNodeWithTag(IntakeTestTags.IRS_TOGGLE).assertDoesNotExist()
        composeRule.onNodeWithTag(IntakeTestTags.LLINS_AVAILABLE).assertDoesNotExist()
    }

    @Test
    fun intake_e02_months_since_irs_hidden_when_toggle_false() {
        val formOffState = IntakeState(
            surveillanceForm = SurveillanceForm(
                numPeopleSleptInHouse = 0,
                wasIrsConducted = false,
                monthsSinceIrs = 5,
                numLlinsAvailable = 0,
                llinType = null,
                llinBrand = null,
                numPeopleSleptUnderLlin = null,
                submittedAt = null
            )
        )
        launchIntake(formOffState)

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

        composeRule.onNodeWithTag(IntakeTestTags.TILE_FORM).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.IRS_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.MONTHS_SINCE_IRS).assertDoesNotExist()
    }

    @Test
    fun intake_e03_months_since_irs_visible_when_toggle_true() {
        val formOnState = IntakeState(
            surveillanceForm = SurveillanceForm(
                numPeopleSleptInHouse = 0,
                wasIrsConducted = true,
                monthsSinceIrs = 0,
                numLlinsAvailable = 0,
                llinType = null,
                llinBrand = null,
                numPeopleSleptUnderLlin = null,
                submittedAt = null
            )
        )
        launchIntake(formOnState)

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

        composeRule.onNodeWithTag(IntakeTestTags.MONTHS_SINCE_IRS, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun intake_e04_llin_type_brand_dropdowns_respect_nullability() {
        val anyType = IntakeDropdownOptions.LlinTypeOption.entries.first().label
        val anyBrand = IntakeDropdownOptions.LlinBrandOption.entries.first().label

        run {
            val state = IntakeState(
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 0,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = 1,
                    llinType = null,
                    llinBrand = null,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null
                )
            )
            launchIntake(state)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

            composeRule.onNodeWithTag(IntakeTestTags.LLIN_TYPE_DD).assertDoesNotExist()
            composeRule.onNodeWithTag(IntakeTestTags.LLIN_BRAND_DD).assertDoesNotExist()
        }

        run {
            val state = IntakeState(
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 0,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = 1,
                    llinType = anyType,
                    llinBrand = null,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null
                )
            )
            launchIntake(state)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

            composeRule.onNodeWithTag(IntakeTestTags.LLIN_TYPE_DD, useUnmergedTree = true)
                .assertExists()
            composeRule.onNodeWithTag(IntakeTestTags.LLIN_BRAND_DD).assertDoesNotExist()
        }

        run {
            val state = IntakeState(
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 0,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = 1,
                    llinType = null,
                    llinBrand = anyBrand,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null
                )
            )
            launchIntake(state)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

            composeRule.onNodeWithTag(IntakeTestTags.LLIN_TYPE_DD).assertDoesNotExist()
            composeRule.onNodeWithTag(IntakeTestTags.LLIN_BRAND_DD, useUnmergedTree = true)
                .assertExists()
        }

        run {
            val state = IntakeState(
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 0,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = 1,
                    llinType = anyType,
                    llinBrand = anyBrand,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null
                )
            )
            launchIntake(state)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.TILE_FORM))

            composeRule.onNodeWithTag(IntakeTestTags.LLIN_TYPE_DD, useUnmergedTree = true)
                .assertExists()
            composeRule.onNodeWithTag(IntakeTestTags.LLIN_BRAND_DD, useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun intake_e05_people_in_house_field_present_only_when_form_present() {
        run {
            val withForm = IntakeState(
                surveillanceForm = SurveillanceForm(
                    numPeopleSleptInHouse = 0,
                    wasIrsConducted = false,
                    monthsSinceIrs = null,
                    numLlinsAvailable = 0,
                    llinType = null,
                    llinBrand = null,
                    numPeopleSleptUnderLlin = null,
                    submittedAt = null
                )
            )
            launchIntake(withForm)
            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.NUM_PEOPLE_IN_HOUSE))
            composeRule.onNodeWithTag(IntakeTestTags.NUM_PEOPLE_IN_HOUSE, useUnmergedTree = true)
                .assertExists()
        }

        run {
            val noForm = IntakeState(surveillanceForm = null)
            launchIntake(noForm)
            composeRule.onNodeWithTag(IntakeTestTags.NUM_PEOPLE_IN_HOUSE).assertDoesNotExist()
        }
    }

    // =========================================================================
    // F. Navigation (back, continue)
    // =========================================================================

    @Test
    fun intake_f01_back_navigates_to_landing() {
        launchIntake()
        assertOnIntake()
        composeRule.onNodeWithTag(IntakeTestTags.BACK_ICON).performClick()
        composeRule.waitForIdle()
        assertOnLanding()
    }

    // =========================================================================
    // G. Regression checks
    // =========================================================================

    @Test
    fun intake_g01_geographical_tile_and_notes_always_present() {
        launchIntake()

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.DISTRICT_DD))

        composeRule.onNodeWithTag(IntakeTestTags.DISTRICT_DD, useUnmergedTree = true)
            .assertExists()

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasTestTag(IntakeTestTags.NOTES))

        composeRule.onNodeWithTag(IntakeTestTags.NOTES, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun intake_g02_collection_method_and_specimen_condition_placeholders_present() {
        launchIntake()

        composeRule.onNodeWithTag(IntakeTestTags.COLLECTION_METHOD_DD).assertIsDisplayed()
        composeRule.onNodeWithTag(IntakeTestTags.SPECIMEN_CONDITION_DD).assertIsDisplayed()

        composeRule.onNode(
            hasClickAction() and hasAnyAncestor(hasTestTag(IntakeTestTags.COLLECTION_METHOD_DD)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        composeRule.onNode(
            hasClickAction() and hasAnyAncestor(hasTestTag(IntakeTestTags.SPECIMEN_CONDITION_DD)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun intake_g03_coords_and_error_rows_are_mutually_exclusive() {
        val base = IntakeState()

        run {
            val withCoords = base.copy(
                session = base.session.copy(latitude = 9.9f, longitude = 8.8f),
                locationError = null
            )
            launchIntake(withCoords)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.LOCATION_COORDS_ROW))
            composeRule.onNodeWithTag(IntakeTestTags.LOCATION_COORDS_ROW, useUnmergedTree = true)
                .assertExists()
            composeRule.onNodeWithTag(IntakeTestTags.LOCATION_ERROR_TEXT).assertDoesNotExist()
        }

        run {
            val withError = base.copy(
                session = base.session.copy(latitude = null, longitude = null),
                locationError = IntakeError.LOCATION_GPS_TIMEOUT
            )
            launchIntake(withError)

            composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                .performScrollToNode(hasTestTag(IntakeTestTags.LOCATION_ERROR_TEXT))
            composeRule.onNodeWithTag(IntakeTestTags.LOCATION_ERROR_TEXT, useUnmergedTree = true)
                .assertExists()
            composeRule.onNodeWithTag(IntakeTestTags.LOCATION_COORDS_ROW).assertDoesNotExist()
        }
    }
}
