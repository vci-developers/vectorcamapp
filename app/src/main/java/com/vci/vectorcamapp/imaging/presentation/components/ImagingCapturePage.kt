package com.vci.vectorcamapp.imaging.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.empty.EmptySpace
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.imaging.presentation.components.camera.ImagingCameraBinding
import com.vci.vectorcamapp.imaging.presentation.components.icon.AnimatedArrowIcon
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ImagingCapturePage(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    page: Int,
    pagerState: PagerState,
    scope: CoroutineScope,
    cameraBinding: ImagingCameraBinding,
    modifier: Modifier = Modifier,
) {
    if (state.showExitDialog) {
        ImagingExitDialog(state = state, onAction = onAction)
    }
    if (state.currentSpecimen.shouldProcessFurther) {
        FurtherProcessingDialog(state = state, onAction = onAction, modifier = modifier)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(vertical = MaterialTheme.dimensions.paddingMedium)
                .fillMaxHeight()
                .width(IntrinsicSize.Max),
        ) {
            CaptureHeader(
                state = state,
                page = page,
                onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onAction = onAction,
            )
            if (state.currentImageBytes != null) {
                CapturedImageReview(state = state, onAction = onAction)
            } else {
                LiveCapturePanel(state = state, onAction = onAction, cameraBinding = cameraBinding)
            }
        }
    }
}

@Composable
private fun CaptureHeader(
    state: ImagingState,
    page: Int,
    onPrevious: () -> Unit,
    onAction: (ImagingAction) -> Unit,
) {
    val isReviewingCapture = state.currentImageBytes != null
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = MaterialTheme.dimensions.paddingSmall,
                horizontal = MaterialTheme.dimensions.paddingMedium,
            ),
    ) {
        if (isReviewingCapture) {
            RetakeButton(onClick = { onAction(ImagingAction.RetakeImage) })
        } else {
            AnimatedArrowIcon(isLeft = true, enabled = page > 0, onClick = onPrevious)
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingSmall))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                color = MaterialTheme.colors.accent,
                shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            ),
        ) {
            Text(
                text = stringResource(R.string.imaging_label_specimen_number, page + 1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colors.textPrimary,
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingSmall))
        if (isReviewingCapture) {
            EmptySpace(
                width = MaterialTheme.dimensions.iconSizeExtraLarge,
                height = MaterialTheme.dimensions.iconSizeExtraLarge,
            )
        } else {
            ExitButton(state = state, onClick = { onAction(ImagingAction.ShowExitDialog) })
        }
    }
}

@Composable
private fun RetakeButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MaterialTheme.dimensions.componentHeightMedium)
            .background(color = MaterialTheme.colors.error, shape = CircleShape),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cancel),
                contentDescription = stringResource(R.string.imaging_content_description_delete),
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge),
            )
        }
    }
}

@Composable
private fun ExitButton(
    state: ImagingState,
    onClick: () -> Unit,
) {
    val isReturn = state.sessionUnitId != null
    Box(
        modifier = Modifier
            .size(MaterialTheme.dimensions.componentHeightMedium)
            .background(color = MaterialTheme.colors.successConfirm, shape = CircleShape),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_exit),
                contentDescription = stringResource(
                    if (isReturn) {
                        R.string.imaging_content_description_back_to_batches
                    } else {
                        R.string.imaging_content_description_exit
                    },
                ),
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge),
            )
        }
    }
}
