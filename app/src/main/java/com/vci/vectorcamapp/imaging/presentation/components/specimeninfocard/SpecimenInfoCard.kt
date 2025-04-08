package com.vci.vectorcamapp.imaging.presentation.components.specimeninfocard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SpecimenInfoCard(
    specimenId: String,
    species: String?,
    sex: String?,
    abdomenStatus: String?,
    onSpecimenIdCorrected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(
                color = Color.White,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Column {
            OutlinedTextField(
                value = specimenId,
                onValueChange = onSpecimenIdCorrected,
                label = { Text("Specimen ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            species?.let {
                Spacer(modifier = Modifier.height(12.dp))

                LabelRow("Species: ", species, modifier = modifier)
            }

            sex?.let {
                Spacer(modifier = Modifier.height(12.dp))

                LabelRow("Sex: ", sex, modifier = modifier)
            }

            abdomenStatus?.let {
                Spacer(modifier = Modifier.height(12.dp))

                LabelRow("Abdomen Status: ", abdomenStatus, modifier = modifier)
            }
        }
    }
}