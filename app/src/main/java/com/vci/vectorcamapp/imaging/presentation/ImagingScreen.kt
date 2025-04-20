package com.vci.vectorcamapp.imaging.presentation

import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.presentation.components.camera.BoundingBoxOverlay
import com.vci.vectorcamapp.imaging.presentation.components.camera.CameraPreview
import com.vci.vectorcamapp.imaging.presentation.components.specimeninfocard.SpecimenInfoCard
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun ImagingScreen(
    state: ImagingState, onAction: (ImagingAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analyzer = remember {
        SpecimenImageAnalyzer { frame ->
            onAction(ImagingAction.ProcessFrame(frame))
        }
    }

    val controller = remember(lifecycleOwner) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
            imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
            imageAnalysisResolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            ).setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
            imageCaptureResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { state.specimens.size + 1 })

    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        if (page < state.specimens.size) {
            val specimen = state.specimens[page]
            AsyncImage(
                model = specimen.imageUri,
                contentDescription = specimen.id,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
            ) {
                if (state.currentImage != null) {
                    Image(
                        bitmap = state.currentImage.asImageBitmap(),
                        contentDescription = "Captured Specimen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    state.currentBoundingBoxUi?.let {
                        BoundingBoxOverlay(it, Modifier.fillMaxSize())
                    }

                    IconButton(
                        onClick = { onAction(ImagingAction.RetakeImage) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(20.dp)
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cancel),
                            contentDescription = "Retake Image",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { onAction(ImagingAction.SaveImageToSession) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_to_session),
                            contentDescription = "Save Image",
                            tint = Color.White
                        )
                    }

                    SpecimenInfoCard(
                        specimenId = state.currentSpecimenId,
                        species = state.currentSpecies,
                        sex = state.currentSex,
                        abdomenStatus = state.currentAbdomenStatus,
                        onSpecimenIdCorrected = {
                            onAction(ImagingAction.CorrectSpecimenId(it))
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {

                    CameraPreview(controller, modifier.fillMaxSize())

                    state.currentBoundingBoxUi?.let {
                        BoundingBoxOverlay(it, Modifier.fillMaxSize())
                    }

                    IconButton(
                        onClick = { onAction(ImagingAction.CaptureImage(controller)) },
                        modifier = Modifier
                            .padding(bottom = 48.dp)
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .align(Alignment.BottomCenter)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Capture Image",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ImagingScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ImagingScreen(
                state = ImagingState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
