package com.vci.vectorcamapp.imaging.presentation.components.specimen.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.presentation.components.specimen.infocard.SpecimenInfoCard

@Composable
fun CapturedSpecimenPage(specimen: Specimen, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = specimen.imageUri,
            contentDescription = specimen.id,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        SpecimenInfoCard(
            specimenId = specimen.id,
            species = specimen.species,
            sex = specimen.sex,
            abdomenStatus = specimen.abdomenStatus,
            onSpecimenIdCorrected = {},
            modifier = modifier.align(Alignment.BottomCenter)
        )
    }
}