package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.imaging.presentation.components.specimen.infocard.SpecimenInfoCard

@Composable
fun CapturedSpecimenReviewPage(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            bitmap = state.currentImage!!.asImageBitmap(),
            contentDescription = "Captured Specimen",
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        state.currentBoundingBoxUi?.let {
            BoundingBoxOverlay(it, modifier.fillMaxSize())
        }

        IconButton(
            onClick = { onAction(ImagingAction.RetakeImage) },
            modifier = modifier
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
            modifier = modifier
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
            modifier = modifier.align(Alignment.BottomCenter)
        )
    }
}