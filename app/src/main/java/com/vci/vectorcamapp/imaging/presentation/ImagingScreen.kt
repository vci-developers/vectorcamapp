package com.vci.vectorcamapp.imaging.presentation

import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vci.vectorcamapp.imaging.presentation.components.camera.CapturedSpecimenReviewPage
import com.vci.vectorcamapp.imaging.presentation.components.camera.LiveCameraPreviewPage
import com.vci.vectorcamapp.imaging.presentation.components.specimen.gallery.CapturedSpecimenPage
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

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { state.specimens.size + 1 }
    )

    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        when {
            page < state.specimens.size -> {
                CapturedSpecimenPage(specimen = state.specimens[page], modifier = modifier)
            }

            state.currentImage != null -> {
                CapturedSpecimenReviewPage(state = state, onAction = onAction, modifier = modifier)
            }

            else -> {
                LiveCameraPreviewPage(
                    controller = controller,
                    boundingBoxUi = state.currentBoundingBoxUi,
                    onImageCaptured = { onAction(ImagingAction.CaptureImage(controller)) },
                    modifier = modifier
                )
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
