package com.vci.vectorcamapp.imaging.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.imaging.presentation.components.camera.ImagingCameraBinding
import com.vci.vectorcamapp.imaging.presentation.components.camera.LiveCameraPreview
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun ColumnScope.LiveCapturePanel(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    cameraBinding: ImagingCameraBinding,
) {
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
            .padding(MaterialTheme.dimensions.paddingMedium)
            .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)),
        contentAlignment = Alignment.Center,
    ) {
        LiveCameraPreview(
            surfaceRequest = cameraBinding.surfaceRequest,
            camera = cameraBinding.camera,
            inferenceResults = if (state.shouldRunInference) state.previewInferenceResults else emptyList(),
            focusPoint = state.focusPoint,
            onFocusAt = { onAction(ImagingAction.FocusAt(it)) },
            onCancelFocus = { onAction(ImagingAction.CancelFocus) },
            modifier = Modifier.fillMaxSize(),
            isManualFocusing = state.isManualFocusing,
            captureStage = state.captureStage,
            capturePreviewBitmap = state.capturePreviewBitmap,
        )
    }
    Column(modifier = Modifier.height(MaterialTheme.dimensions.componentHeightExtraExtraExtraLarge)) {
        if (state.sessionType == SessionType.PRACTICE) {
            PracticeCaptureWarning()
        }
        InfoTile {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = MaterialTheme.dimensions.paddingExtraLarge),
            ) {
                if (state.allowModelInferenceToggle) {
                    ToggleField(
                        label = stringResource(R.string.imaging_label_run_inference),
                        checked = state.shouldRunInference,
                        onCheckedChange = { onAction(ImagingAction.ToggleModelInference(it)) },
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                }
                val specimenId = state.currentSpecimen.id
                Text(
                    text = if (specimenId == "") {
                        stringResource(R.string.imaging_label_specimen_id_placeholder)
                    } else {
                        specimenId
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (specimenId == "") {
                        MaterialTheme.colors.textSecondary
                    } else {
                        MaterialTheme.colors.textPrimary
                    },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                ActionButton(
                    label = stringResource(R.string.imaging_action_capture),
                    onClick = {
                        cameraBinding.imageCapture?.let { imageCapture ->
                            onAction(
                                ImagingAction.CaptureImage(
                                    imageCapture,
                                    cameraBinding.metadataListener.latestMetadata,
                                ),
                            )
                        }
                    },
                    iconPainter = painterResource(id = R.drawable.ic_camera),
                    enabled = state.captureStage == null && state.isCameraReady,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PracticeCaptureWarning() {
    Surface(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.warningBackground,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = MaterialTheme.colors.warning,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium),
            )
            Text(
                text = stringResource(R.string.imaging_body_practice_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textPrimary,
            )
        }
    }
}
