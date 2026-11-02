package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.camera.BindBarcodeScanner
import com.vci.vectorcamapp.core.presentation.components.camera.CameraPreview
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.imaging.domain.use_cases.extractPrintedSpecimenId
import com.vci.vectorcamapp.imaging.presentation.components.ImagingCapturePage
import com.vci.vectorcamapp.imaging.presentation.components.camera.rememberImagingCameraBinding
import com.vci.vectorcamapp.imaging.presentation.components.specimen.SpecimenReviewPage
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun ImagingScreen(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Temporary prototype: confirmed codes use the existing specimen persistence path.
    var scanning by remember { mutableStateOf(state.confirmedScannedSpecimenId == null) }
    var cameraReleased by remember { mutableStateOf(state.confirmedScannedSpecimenId != null) }
    var rearCameraReleased by remember { mutableStateOf(true) }
    var tubeId by remember { mutableStateOf<String?>(null) }
    var scannerSurface by remember { mutableStateOf<SurfaceRequest?>(null) }
    LaunchedEffect(state.confirmedScannedSpecimenId) {
        if (state.confirmedScannedSpecimenId != null) {
            tubeId = null
            scanning = false
            rearCameraReleased = false
        } else if (tubeId == null) {
            cameraReleased = false
            scanning = true
        }
    }
    RearCameraImagingScreen(
        state = state,
        onAction = onAction,
        modifier = modifier.fillMaxSize(),
        rearCameraEnabled = cameraReleased && state.confirmedScannedSpecimenId != null,
        onCameraReleased = { rearCameraReleased = true },
        scannerPreview = if (state.confirmedScannedSpecimenId == null) {
            {
                CameraPreview(
                    surfaceRequest = scannerSurface,
                    modifier = Modifier.fillMaxSize(),
                    overlay = {
                        Text(
                            text = "Hold the tube barcode in front of the front camera.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.buttonText,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .background(MaterialTheme.colors.overlayColor)
                                .padding(MaterialTheme.dimensions.paddingMedium),
                        )
                    },
                )
            }
        } else null,
    )
    if (scanning && rearCameraReleased) {
        BindBarcodeScanner(
            onSurfaceRequest = { scannerSurface = it },
            qrOnly = false,
            transformValue = { value ->
                value.takeIf { it.startsWith("KE") && it.length == 6 }
            },
            transformRecognizedText = ::extractPrintedSpecimenId,
            onBarcodeScanned = {
                tubeId = it
                scanning = false
            },
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onCameraReleased = { cameraReleased = true },
        )
    } else if (!scanning && cameraReleased && tubeId != null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colors.cardBackground,
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            title = {
                Text(
                    "Confirm specimen ID",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                ) {
                    Text(
                        "Please check the ID against the tube label.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textSecondary,
                    )
                    TextEntryField(
                        label = stringResource(R.string.imaging_label_specimen_id),
                        value = tubeId.orEmpty(),
                        onValueChange = { tubeId = it },
                        singleLine = true,
                        error = state.specimenIdError,
                        showErrorMessage = false,
                    )
                    if (state.specimenIdError != null) {
                        Text(
                            text = "Please enter a six-character specimen ID that starts with KE.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.error,
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ActionButton(
                        label = "Confirm & continue",
                        onClick = {
                            onAction(ImagingAction.ConfirmScannedSpecimenId(tubeId.orEmpty()))
                        },
                    )
                    TextButton(onClick = {
                        cameraReleased = false
                        scannerSurface = null
                        tubeId = null
                        scanning = true
                    }) {
                        Text(
                            "Scan again",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colors.textPrimary,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun RearCameraImagingScreen(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    modifier: Modifier = Modifier,
    rearCameraEnabled: Boolean,
    scannerPreview: (@Composable () -> Unit)?,
    onCameraReleased: () -> Unit,
) {
    val cameraBinding = rememberImagingCameraBinding(
        isReviewing = state.currentImageBytes != null,
        onAction = onAction,
        isEnabled = rearCameraEnabled,
        onCameraReleased = onCameraReleased,
    )
    val scope = rememberCoroutineScope()
    val specimenCount = state.specimensWithImagesAndInferenceResults.size
    val pagerState = rememberPagerState(
        initialPage = specimenCount,
        pageCount = { state.specimensWithImagesAndInferenceResults.size + 1 },
    )

    LaunchedEffect(specimenCount) {
        pagerState.scrollToPage(specimenCount)
    }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        if (page < state.specimensWithImagesAndInferenceResults.size) {
            SpecimenReviewPage(
                state = state,
                page = page,
                pagerState = pagerState,
                scope = scope,
                modifier = modifier,
            )
        } else {
            ImagingCapturePage(
                state = state,
                onAction = onAction,
                page = page,
                pagerState = pagerState,
                scope = scope,
                cameraBinding = cameraBinding,
                modifier = modifier,
                scannerPreview = scannerPreview,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun ImagingScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ImagingScreen(
                state = ImagingState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
