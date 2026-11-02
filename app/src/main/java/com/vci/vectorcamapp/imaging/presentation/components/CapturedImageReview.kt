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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.core.presentation.extensions.displayTextFor
import com.vci.vectorcamapp.imaging.domain.enums.AbdomenStatusLabel
import com.vci.vectorcamapp.imaging.domain.enums.SexLabel
import com.vci.vectorcamapp.imaging.domain.enums.SpeciesLabel
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.imaging.presentation.components.specimen.SpecimenImageOverlay
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun ColumnScope.CapturedImageReview(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    var isImageLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
            .padding(MaterialTheme.dimensions.paddingMedium)
            .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)),
        contentAlignment = Alignment.Center,
    ) {
        SpecimenImageOverlay(
            inferenceResult = state.currentInferenceResult,
            showBoundingBoxOverlay = isImageLoaded,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.currentImageBytes)
                    .crossfade(true)
                    .build(),
                contentDescription = state.currentSpecimen.id,
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoaded = true },
                onError = { isImageLoaded = false },
                error = painterResource(R.drawable.specimen_image_placeholder_not_uploaded),
                fallback = painterResource(R.drawable.specimen_image_placeholder_not_uploaded),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    InfoTile(modifier = Modifier.height(MaterialTheme.dimensions.componentHeightExtraExtraExtraLarge)) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    vertical = MaterialTheme.dimensions.paddingExtraLarge,
                    horizontal = MaterialTheme.dimensions.paddingExtraLarge,
                ),
        ) {
            TextEntryField(
                label = stringResource(R.string.imaging_label_specimen_id),
                value = state.currentSpecimen.id,
                onValueChange = { onAction(ImagingAction.CorrectSpecimenId(it)) },
                singleLine = true,
                error = state.specimenIdError,
                showErrorMessage = false,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            InferenceSummary(state = state, onSave = { onAction(ImagingAction.SaveImageToSession) })
        }
    }
}

@Composable
private fun InferenceSummary(
    state: ImagingState,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val specimenImage = state.currentSpecimenImage
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.paddingSmall)) {
            SpeciesLabel.displayTextFor(context, specimenImage.species)?.let { species ->
                SummaryLine(stringResource(R.string.imaging_label_species, species))
            }
            SexLabel.displayTextFor(context, specimenImage.sex)?.let { sex ->
                SummaryLine(stringResource(R.string.imaging_label_sex, sex))
            }
            AbdomenStatusLabel.displayTextFor(context, specimenImage.abdomenStatus)?.let { abdomen ->
                SummaryLine(stringResource(R.string.imaging_label_abdomen_status, abdomen))
            }
        }
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colors.successConfirm),
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
        ) {
            Text(
                text = stringResource(R.string.imaging_action_save),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.buttonText,
            )
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colors.textPrimary,
    )
}
