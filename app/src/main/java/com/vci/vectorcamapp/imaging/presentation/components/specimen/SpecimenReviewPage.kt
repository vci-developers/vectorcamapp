package com.vci.vectorcamapp.imaging.presentation.components.specimen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.imaging.presentation.components.icon.AnimatedArrowIcon
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun SpecimenReviewPage(
    state: ImagingState,
    page: Int,
    pagerState: PagerState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val specimen = state.specimensWithImagesAndInferenceResults[page]
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxHeight().wrapContentWidth(),
        ) {
            ReviewPagerHeader(
                page = page,
                total = state.specimensWithImagesAndInferenceResults.size,
                onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
            )
            LazyColumn(
                modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val imageList = specimen.specimenImagesAndInferenceResults
                itemsIndexed(imageList) { index, (specimenImage, inferenceResult) ->
                    CapturedSpecimenTile(
                        specimen = specimen.specimen,
                        specimenImage = specimenImage,
                        inferenceResult = inferenceResult,
                        badgeText = stringResource(
                            R.string.imaging_label_image_badge,
                            index + 1,
                            imageList.size,
                        ),
                        modifier = Modifier.fillParentMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewPagerHeader(
    page: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = MaterialTheme.dimensions.paddingMedium)
            .fillMaxWidth(),
    ) {
        AnimatedArrowIcon(isLeft = true, enabled = page > 0, onClick = onPrevious)
        Box(
            modifier = Modifier.background(
                color = MaterialTheme.colors.accent,
                shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            ),
        ) {
            Text(
                text = stringResource(R.string.imaging_label_specimen_of_total, page + 1, total),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colors.textPrimary,
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            )
        }
        AnimatedArrowIcon(isLeft = false, enabled = true, onClick = onNext)
    }
}
