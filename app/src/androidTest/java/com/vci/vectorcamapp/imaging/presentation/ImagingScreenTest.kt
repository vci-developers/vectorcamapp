package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.imaging.presentation.util.ImagingTestTags
import com.vci.vectorcamapp.navigation.Destination
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

@HiltAndroidTest
class ImagingScreenTest {
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

    // =========================
    // Harness / Helpers
    // =========================

    private fun jpegBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    private fun specimen(id: String) = Specimen(id = id, remoteId = null)

    private fun specimenImage(
        species: String = "Anopheles",
        sex: String = "F",
        abdomenStatus: String = "Fed",
        capturedAt: Long = System.currentTimeMillis()
    ): SpecimenImage = SpecimenImage(
        localId = UUID.randomUUID().toString(),
        remoteId = null,
        species = species,
        sex = sex,
        abdomenStatus = abdomenStatus,
        imageUri = android.net.Uri.EMPTY,
        imageUploadStatus = UploadStatus.NOT_STARTED,
        metadataUploadStatus = UploadStatus.NOT_STARTED,
        capturedAt = capturedAt,
        submittedAt = null
    )

    private fun savedPage(
        id: String,
        inference: InferenceResult? = null,
        image: SpecimenImage = specimenImage()
    ) = SpecimenWithSpecimenImagesAndInferenceResults(
        specimen = specimen(id),
        specimenImagesAndInferenceResults = listOf(
            SpecimenImageAndInferenceResult(
                specimenImage = image,
                inferenceResult = inference
            )
        )
    )

    private fun swipePagerRight() {
        composeRule.onNodeWithTag(ImagingTestTags.PAGER).performTouchInput {
            val w = width.toFloat()
            val h = height.toFloat()
            val y = h / 2f
            down(Offset(w * 0.15f, y))
            moveTo(Offset(w * 0.85f, y))
            up()
        }
    }

    private fun swipePagerLeft() {
        composeRule.onNodeWithTag(ImagingTestTags.PAGER).performTouchInput {
            val w = width.toFloat()
            val h = height.toFloat()
            val y = h / 2f
            down(Offset(w * 0.85f, y))
            moveTo(Offset(w * 0.15f, y))
            up()
        }
    }

    private fun waitUntilTagExists(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun launchImagingScreen(
        initial: ImagingState = ImagingState(),
        onActionIntercept: ((ImagingAction) -> Unit)? = null
    ) {
        composeRule.activity.setContent {
            var state by remember { mutableStateOf(initial) }

            VectorcamappTheme {
                NavHost(
                    navController = navController,
                    startDestination = Destination.Imaging
                ) {
                    composable<Destination.Imaging> {
                        BaseScaffold(modifier = Modifier.fillMaxSize()) { inner ->
                            ImagingScreen(
                                state = state,
                                onAction = { a ->
                                    onActionIntercept?.invoke(a)
                                    when (a) {
                                        ImagingAction.ShowExitDialog ->
                                            state = state.copy(showExitDialog = true)

                                        ImagingAction.DismissExitDialog ->
                                            state = state.copy(showExitDialog = false, pendingAction = null)

                                        is ImagingAction.SelectPendingAction ->
                                            state = state.copy(pendingAction = a.pendingAction)

                                        ImagingAction.ClearPendingAction ->
                                            state = state.copy(pendingAction = null)

                                        ImagingAction.ConfirmPendingAction -> {
                                            val pending = state.pendingAction
                                            state = state.copy(showExitDialog = false, pendingAction = null)

                                            if (pending is ImagingAction.SaveSessionProgress ||
                                                pending is ImagingAction.SubmitSession
                                            ) {
                                                navController.navigate(Destination.Landing)
                                            }
                                        }

                                        is ImagingAction.FocusAt ->
                                            state = state.copy(focusPoint = a.offset, isManualFocusing = true)

                                        ImagingAction.CancelFocus ->
                                            state = state.copy(focusPoint = null, isManualFocusing = false)

                                        ImagingAction.RetakeImage ->
                                            state = state.copy(currentImageBytes = null)

                                        is ImagingAction.CorrectSpecimenId ->
                                            state = state.copy(
                                                currentSpecimen = state.currentSpecimen.copy(id = a.specimenId)
                                            )

                                        ImagingAction.SaveImageToSession -> {
                                            val newSavedPage = savedPage(state.currentSpecimen.id.ifEmpty { "S-NEW" })
                                            state = state.copy(
                                                currentImageBytes = null,
                                                currentSpecimen = Specimen(id = "", remoteId = null),
                                                specimensWithImagesAndInferenceResults = state.specimensWithImagesAndInferenceResults + newSavedPage
                                            )
                                        }

                                        else -> Unit
                                    }
                                },
                                modifier = Modifier.padding(inner)
                            )
                        }
                    }
                    composable<Destination.Landing> { }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertOnImaging() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route)
            .isEqualTo(Destination.Imaging::class.qualifiedName)
    }

    private fun assertOnLanding() {
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route)
            .isEqualTo(Destination.Landing::class.qualifiedName)
    }

    // =========================
    // A. Static UI (camera page)
    // =========================

    @Test
    fun img_a01_cameraPage_showsPreview_andExit_andCaptureDisabledWhenNotReady() {
        launchImagingScreen(initial = ImagingState(isCameraReady = false))
        composeRule.onNodeWithTag(ImagingTestTags.CAMERA_PREVIEW).assertExists()
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).assertExists().assertHasClickAction()
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_CAPTURE).assertExists().assertIsNotEnabled()
        composeRule.onNodeWithText("Specimen 1 in this Session").assertIsDisplayed()
        composeRule.onNodeWithText("Specimen ID will appear here").assertIsDisplayed()
    }

    @Test
    fun img_a02_captureEnabled_whenCameraReady() {
        var last: ImagingAction? = null
        launchImagingScreen(initial = ImagingState(isCameraReady = true)) { action -> last = action }
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_CAPTURE).assertIsEnabled().performClick()
        assertThat(last is ImagingAction.CaptureImage).isTrue()
    }

    @Test fun img_a03_processingDisablesCaptureEvenWhenReady() {
        launchImagingScreen(initial = ImagingState(isCameraReady = true, isProcessing = true))
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_CAPTURE).assertIsNotEnabled()
    }

    @Test fun img_a04_tapPreviewShowsAndCancelsManualFocus() {
        launchImagingScreen(initial = ImagingState(isCameraReady = true))
        composeRule.onNodeWithTag(ImagingTestTags.CAMERA_PREVIEW).performTouchInput { click(center) }
        composeRule.onNodeWithTag(ImagingTestTags.MANUAL_FOCUS_RING).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.MANUAL_FOCUS_RING).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.MANUAL_FOCUS_RING).assertDoesNotExist()
    }

    @Test
    fun img_a05_cameraPage_displaysSpecimenIdWhenPresent() {
        val specimenId = "AUTO-DETECTED-ID"
        launchImagingScreen(
            initial = ImagingState(
                currentSpecimen = Specimen(id = specimenId, remoteId = null)
            )
        )

        composeRule.onNodeWithText("Specimen ID will appear here").assertDoesNotExist()
        composeRule.onNodeWithText(specimenId).assertIsDisplayed()
    }

    // =========================
    // B. Captured UI
    // =========================

    @Test
    fun img_b01_capturedState_showsSave_buttonsInvokeActions() {
        var actions = mutableListOf<ImagingAction>()
        launchImagingScreen(
            initial = ImagingState(currentImageBytes = jpegBytes())
        ) { action ->
            actions.add(action)
        }

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_SAVE_TO_SESSION).assertIsDisplayed().performClick()

        assertThat(actions.any { it == ImagingAction.SaveImageToSession }).isTrue()
    }

    @Test
    fun img_b02_retake_returnsToCameraPage() {
        launchImagingScreen(initial = ImagingState(currentImageBytes = jpegBytes()))
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_RETAKE).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_CAPTURE).assertExists()
    }

    @Test
    fun img_b03_capturedState_showsEditableSpecimenId_andWarning() {
        var lastAction: ImagingAction? = null
        val initialId = "ID-123"
        val correctedId = "ID-456"

        launchImagingScreen(
            initial = ImagingState(
                currentImageBytes = jpegBytes(),
                currentSpecimen = Specimen(id = initialId, remoteId = null)
            )
        ) { action ->
            if (action is ImagingAction.CorrectSpecimenId) {
                lastAction = action
            }
        }

        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_TEXT_FIELD_SPECIMEN_ID).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_PILL_CORRECT_ID_WARNING).assertIsDisplayed()

        composeRule.onNodeWithText("Specimen ID").assertIsDisplayed()
        composeRule.onNodeWithText(initialId).assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_TEXT_FIELD_SPECIMEN_ID)
            .performTextClearance()
        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_TEXT_FIELD_SPECIMEN_ID)
            .performTextInput(correctedId)

        assertThat(lastAction).isInstanceOf(ImagingAction.CorrectSpecimenId::class.java)
        assertThat((lastAction as ImagingAction.CorrectSpecimenId).specimenId).isEqualTo(correctedId)
    }

    @Test
    fun img_b04_saveImageToSession_returnsToCameraPage() {
        launchImagingScreen(initial = ImagingState(currentImageBytes = jpegBytes()))

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_SAVE_TO_SESSION).performClick()

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_CAPTURE).assertExists()
        composeRule.onNodeWithTag(ImagingTestTags.CAMERA_PREVIEW).assertExists()

        composeRule.onNodeWithText("Specimen 2 in this Session").assertIsDisplayed()
    }

    // =========================
    // C. Pager pages (saved specimens)
    // =========================

    @Test
    fun img_c01_swipeThroughSavedPages_showsSpecimenHeaders() {
        val saved = listOf(savedPage("S-001"), savedPage("S-002"))
        launchImagingScreen(initial = ImagingState(specimensWithImagesAndInferenceResults = saved))

        composeRule.onNodeWithText("Specimen 3 in this Session").assertIsDisplayed()

        swipePagerRight()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Specimen 2 of 2 in this Session").assertIsDisplayed()

        swipePagerRight()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Specimen 1 of 2 in this Session").assertIsDisplayed()
    }

    @Test fun img_c02_swipePastFirst_doesNotChangePage() {
        val saved = listOf(savedPage("S-001"), savedPage("S-002"))
        launchImagingScreen(initial = ImagingState(specimensWithImagesAndInferenceResults = saved))
        swipePagerRight(); swipePagerRight()
        composeRule.onNodeWithText("Specimen 1 of 2 in this Session").assertIsDisplayed()
        swipePagerRight()
        composeRule.onNodeWithText("Specimen 1 of 2 in this Session").assertIsDisplayed()
    }

    @Test fun img_c03_savedToCapture_swipeNextShowsCaptureHeader() {
        val saved = listOf(savedPage("S-001"))
        launchImagingScreen(initial = ImagingState(specimensWithImagesAndInferenceResults = saved))
        swipePagerRight()
        composeRule.onNodeWithText("Specimen 1 of 1 in this Session").assertIsDisplayed()
        swipePagerLeft()
        composeRule.onNodeWithText("Specimen 2 in this Session").assertIsDisplayed()
    }

    @Test
    fun img_c04_savedPage_displaysCorrectContentAndBadge() {
        val testTimestamp = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(testTimestamp)

        val image = specimenImage(
            species = "Aedes aegypti",
            sex = "Male",
            abdomenStatus = "Unfed",
            capturedAt = testTimestamp
        )
        val saved = listOf(savedPage("S-001", image = image))

        launchImagingScreen(initial = ImagingState(specimensWithImagesAndInferenceResults = saved))

        swipePagerRight()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ImagingTestTags.SPECIMEN_CAPTURED_SPECIMEN_TILE).assertIsDisplayed()
        composeRule.onNodeWithText("Specimen ID: S-001").assertIsDisplayed()
        composeRule.onNodeWithText("Species: Aedes aegypti").assertIsDisplayed()
        composeRule.onNodeWithText("Sex: Male").assertIsDisplayed()
        composeRule.onNodeWithText("Abdomen Status: Unfed").assertIsDisplayed()
        composeRule.onNodeWithText("Captured At: $formattedDate").assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.SPECIMEN_BADGE_IMAGE_INDEX).assertIsDisplayed()
        composeRule.onNodeWithText("1 of 1").assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_TEXT_FIELD_SPECIMEN_ID).assertDoesNotExist()
        composeRule.onNodeWithTag(ImagingTestTags.CAPTURED_SPECIMEN_PILL_CORRECT_ID_WARNING).assertDoesNotExist()
    }

    // =========================
    // D. Exit dialog flow
    // =========================

    @Test
    fun img_d01_exitDialog_open_selectSubmit_confirm_back_and_close() {
        launchImagingScreen(initial = ImagingState())
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).performClick()
        waitUntilTagExists(ImagingTestTags.EXIT_DIALOG)
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_DIALOG).assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).assertIsDisplayed().performClick()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_CONFIRM).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_BACK).assertIsDisplayed().performClick()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SAVE).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SAVE).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_CONFIRM).performClick()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_DIALOG).assertDoesNotExist()
    }

    @Test
    fun img_d02_exitDialog_dismissViaOutsideOrDismissButton() {
        launchImagingScreen(initial = ImagingState())
        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_DIALOG).assertIsDisplayed()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SAVE).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_BACK).performClick()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun nav_e01_exit_selectSubmit_confirm_navigatesToLanding() {
        launchImagingScreen()

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).assertExists().performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_CONFIRM).assertIsDisplayed().performClick()

        assertOnLanding()
    }

    @Test
    fun nav_e02_exit_selectSave_confirm_navigatesToLanding() {
        launchImagingScreen()

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SAVE).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_CONFIRM).assertIsDisplayed().performClick()

        assertOnLanding()
    }

    @Test
    fun nav_e03_backFromConfirm_returnsToSelect_and_noNavigation() {
        launchImagingScreen()

        composeRule.onNodeWithTag(ImagingTestTags.BUTTON_EXIT).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).performClick()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_BACK).performClick()

        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SAVE).assertIsDisplayed()
        composeRule.onNodeWithTag(ImagingTestTags.EXIT_SELECT_SUBMIT).assertIsDisplayed()
        assertOnImaging()
    }
}
