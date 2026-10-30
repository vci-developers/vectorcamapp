package com.vci.vectorcamapp.imaging.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.imaging.presentation.components.ImagingCapturePage
import com.vci.vectorcamapp.imaging.presentation.components.camera.rememberImagingCameraBinding
import com.vci.vectorcamapp.imaging.presentation.components.specimen.SpecimenReviewPage
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun ImagingScreen(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraBinding = rememberImagingCameraBinding(
        isReviewing = state.currentImageBytes != null,
        onAction = onAction,
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
