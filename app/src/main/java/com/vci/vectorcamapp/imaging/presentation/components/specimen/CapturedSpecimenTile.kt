package com.vci.vectorcamapp.imaging.presentation.components.specimen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CapturedSpecimenTile(
    specimen: Specimen,
    specimenImage: SpecimenImage,
    inferenceResult: InferenceResult?,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
) {
    val context = LocalContext.current
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    InfoTile(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(vertical = MaterialTheme.dimensions.paddingLarge)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimensions.paddingLarge)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                SpecimenImageOverlay(
                    inferenceResult = inferenceResult
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(specimenImage.imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = specimen.id,
                        contentScale = ContentScale.Fit
                    )
                }

                badgeText?.let {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MaterialTheme.dimensions.paddingSmall),
                        containerColor = MaterialTheme.colors.info,
                        contentColor = MaterialTheme.colors.buttonText
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(MaterialTheme.dimensions.paddingExtraSmall)
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall),
                modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.paddingLarge)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall),
                    modifier = Modifier.height(MaterialTheme.dimensions.componentHeightSmall)
                ) {
                    VerticalDivider(
                        thickness = MaterialTheme.dimensions.dividerThickness,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxHeight()
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_specimen),
                        contentDescription = "Mosquito",
                        tint = MaterialTheme.colors.icon,
                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                    )

                    Text(
                        text = "Specimen ID: ${specimen.id}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                Text(
                    text = if (specimenImage.species != null) "Species: ${specimenImage.species}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = if (specimenImage.sex != null) "Sex: ${specimenImage.sex}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = if (specimenImage.abdomenStatus != null) "Abdomen Status: ${specimenImage.abdomenStatus}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colors.textPrimary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colors.divider,
                thickness = MaterialTheme.dimensions.dividerThickness
            )

            Text(
                text = "Captured At: ${dateTimeFormatter.format(specimenImage.capturedAt)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colors.textPrimary,
                modifier = Modifier.padding(
                    start = MaterialTheme.dimensions.paddingLarge,
                    end = MaterialTheme.dimensions.paddingLarge,
                    top = MaterialTheme.dimensions.paddingSmall
                )
            )
        }
    }
}
