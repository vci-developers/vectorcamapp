package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.complete_session.details.presentation.util.matchesQuery
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun CompleteSessionSpecimens(
    session: Session,
    specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    modifier: Modifier = Modifier
) {
    if (specimensWithImagesAndInferenceResults.isEmpty()) {
        Text(
            "No specimens were captured during this session.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxSize()
        )
    } else {
        var searchQuery by remember { mutableStateOf("") }

        val allImages = remember(specimensWithImagesAndInferenceResults) {
            specimensWithImagesAndInferenceResults.asReversed().flatMap { specimenWithImages ->
                val specimen = specimenWithImages.specimen
                val imageList = specimenWithImages.specimenImagesAndInferenceResults
                val totalImages = imageList.size
                imageList.mapIndexed { index, (specimenImage, _) ->
                    Triple(specimen, specimenImage, "${index + 1} of $totalImages")
                }
            }
        }

        val filteredImages = remember(searchQuery, allImages) {
            if (searchQuery.isBlank()) {
                allImages
            } else {
                allImages.filter { (specimen, specimenImage, _) ->
                    matchesQuery(searchQuery, specimen, specimenImage)
                }
            }
        }

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by specimen ID, species, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (filteredImages.isEmpty()) {
                Text(
                    text = "No matching specimens found.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(
                        items = filteredImages,
                        key = { (_, specimenImage, _) -> specimenImage.localId }
                    ) { (specimen, specimenImage, badgeText) ->
                        CompleteSessionSpecimensTile(
                            session = session,
                            specimen = specimen,
                            specimenImage = specimenImage,
                            badgeText = badgeText,
                            modifier = Modifier.width(
                                screenWidthFraction(0.9f)
                            )
                        )
                    }
                }
            }
        }
    }
}
