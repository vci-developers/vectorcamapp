package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.ui.extensions.color
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.extensions.displayText
import java.text.SimpleDateFormat
import java.util.Locale
import com.vci.vectorcamapp.ui.extensions.zoomPanGesture

/* TODO: CLEANUP */
@Composable
fun CompleteSessionSpecimensTile(
    specimen: Specimen,
    specimenImage: SpecimenImage,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    batchName: String? = null,
) {

    val context = LocalContext.current
    val density = LocalDensity.current
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    val fallbackPainter = if (specimenImage.imageUploadStatus == UploadStatus.COMPLETED) {
        painterResource(R.drawable.specimen_image_placeholder_uploaded)
    } else {
        painterResource(R.drawable.specimen_image_placeholder_not_uploaded)
    }

    InfoTile(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
                .clip(RectangleShape)
        ) {
            val containerSize = IntSize(
                width = with(density) { maxWidth.roundToPx() },
                height = with(density) { maxHeight.roundToPx() }
            )

            AsyncImage(
                model = ImageRequest.Builder(context).data(specimenImage.imageUri).build(),
                contentDescription = stringResource(R.string.complete_session_content_description_specimen_image, specimen.id),
                error = fallbackPainter,
                fallback = fallbackPainter,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomPanGesture(containerSize)
                    .border(
                        width = MaterialTheme.dimensions.borderThicknessThin,
                        color = MaterialTheme.colors.disabled
                    )
            )

            badgeText?.let {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.dimensions.paddingSmall),
                    containerColor = MaterialTheme.colors.info,
                    contentColor = MaterialTheme.colors.buttonText
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(MaterialTheme.dimensions.paddingSmall)
                    )
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall),
                modifier = Modifier.height(MaterialTheme.dimensions.componentHeightSmall)
            ) {
                VerticalDivider(
                    thickness = MaterialTheme.dimensions.dividerThicknessThick,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.fillMaxHeight()
                )

                Icon(
                    painter = painterResource(R.drawable.ic_specimen),
                    contentDescription = stringResource(R.string.complete_session_content_description_mosquito),
                    tint = MaterialTheme.colors.icon,
                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                )

                Text(
                    text = stringResource(R.string.complete_session_label_specimen_id, specimen.id),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            }

            batchName?.let {
                InfoPill(
                    text = stringResource(R.string.complete_session_label_batch_name, it),
                    color = MaterialTheme.colors.info,
                )
            }

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.complete_session_label_metadata_upload_status))
                    withStyle(SpanStyle(color = specimenImage.metadataUploadStatus.color())) {
                        append(specimenImage.metadataUploadStatus.displayText(context))
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.complete_session_label_image_upload_status))
                    withStyle(SpanStyle(color = specimenImage.imageUploadStatus.color())) {
                        append(specimenImage.imageUploadStatus.displayText(context))
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = if (specimenImage.species != null) stringResource(R.string.complete_session_label_species, specimenImage.species) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textPrimary
            )

            Text(
                text = if (specimenImage.sex != null) stringResource(R.string.complete_session_label_sex, specimenImage.sex) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textPrimary
            )

            Text(
                text = if (specimenImage.abdomenStatus != null) stringResource(R.string.complete_session_label_abdomen_status, specimenImage.abdomenStatus) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textPrimary
            )

            Text(
                text = stringResource(R.string.complete_session_label_captured_on, dateTimeFormatter.format(specimenImage.capturedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textPrimary
            )
        }
    }
}

