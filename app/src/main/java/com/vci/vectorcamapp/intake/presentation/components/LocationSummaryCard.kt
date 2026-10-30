package com.vci.vectorcamapp.intake.presentation.components

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

private const val METERS_PER_KILOMETER = 1000f

@Composable
internal fun rememberDistanceToSiteMeters(state: IntakeState): Float? {
    val site = state.selectedSiteWithCoordinates
    val collectionLatitude = state.session.latitude
    val collectionLongitude = state.session.longitude
    val siteLatitude = site?.latitude
    val siteLongitude = site?.longitude
    return remember(collectionLatitude, collectionLongitude, siteLatitude, siteLongitude) {
        if (listOf(collectionLatitude, collectionLongitude, siteLatitude, siteLongitude).any { it == null }) {
            null
        } else {
            val results = FloatArray(1)
            Location.distanceBetween(
                requireNotNull(collectionLatitude).toDouble(),
                requireNotNull(collectionLongitude).toDouble(),
                requireNotNull(siteLatitude).toDouble(),
                requireNotNull(siteLongitude).toDouble(),
                results,
            )
            results[0]
        }
    }
}

@Composable
internal fun LocationSummaryCards(
    state: IntakeState,
    distanceToSiteMeters: Float?,
    onAction: (IntakeAction) -> Unit,
) {
    val site = state.selectedSiteWithCoordinates
    val locations = listOfNotNull(
        Triple(R.string.intake_label_collection_location, state.session.latitude, state.session.longitude),
        site?.takeIf { it.latitude != null && it.longitude != null }?.let {
            Triple(R.string.intake_label_site_location, it.latitude, it.longitude)
        },
    )
    locations.forEachIndexed { index, (title, latitude, longitude) ->
        LocationSummaryCard(
            title = stringResource(title),
            latitude = latitude,
            longitude = longitude,
            state = state,
            distanceToSiteMeters = distanceToSiteMeters,
            isCollectionLocation = index == 0,
            onAction = onAction,
        )
    }
}

@Composable
private fun LocationSummaryCard(
    title: String,
    latitude: Float?,
    longitude: Float?,
    state: IntakeState,
    distanceToSiteMeters: Float?,
    isCollectionLocation: Boolean,
    onAction: (IntakeAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimensions.paddingMedium),
        color = MaterialTheme.colors.appBackground,
        border = BorderStroke(
            MaterialTheme.dimensions.dividerThicknessThin,
            MaterialTheme.colors.fieldBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.textPrimary,
            )
            LocationSummaryBody(
                latitude = latitude,
                longitude = longitude,
                state = state,
                distanceToSiteMeters = distanceToSiteMeters,
                isCollectionLocation = isCollectionLocation,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun LocationSummaryBody(
    latitude: Float?,
    longitude: Float?,
    state: IntakeState,
    distanceToSiteMeters: Float?,
    isCollectionLocation: Boolean,
    onAction: (IntakeAction) -> Unit,
) {
    when {
        latitude != null && longitude != null -> CoordinatesBody(
            latitude = latitude,
            longitude = longitude,
            state = state,
            distanceToSiteMeters = distanceToSiteMeters,
            isCollectionLocation = isCollectionLocation,
            onAction = onAction,
        )
        !state.canUpdateLocation && !state.isLoading -> Text(
            text = stringResource(R.string.intake_body_collection_location_not_recorded),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.textSecondary,
        )
        state.locationError != null -> LocationErrorBody(state = state, onAction = onAction)
        else -> LocationLoadingBody()
    }
}

@Composable
private fun CoordinatesBody(
    latitude: Float,
    longitude: Float,
    state: IntakeState,
    distanceToSiteMeters: Float?,
    isCollectionLocation: Boolean,
    onAction: (IntakeAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)) {
        CoordinateRow(label = stringResource(R.string.intake_label_latitude), value = latitude.toString())
        CoordinateRow(label = stringResource(R.string.intake_label_longitude), value = longitude.toString())
        if (isCollectionLocation) {
            state.session.gpsAccuracy?.let { gpsAccuracy ->
                CoordinateRow(
                    label = stringResource(R.string.intake_label_gps_accuracy),
                    value = stringResource(R.string.intake_value_gps_accuracy, gpsAccuracy),
                )
            }
            if (state.canUpdateLocation) {
                ActionButton(
                    label = stringResource(R.string.intake_action_update_location),
                    onClick = { onAction(IntakeAction.RetryLocation) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            distanceToSiteMeters?.let { distance ->
                CoordinateRow(
                    label = stringResource(R.string.intake_label_distance_to_site),
                    value = formatDistance(distance),
                )
            }
        }
    }
}

@Composable
private fun formatDistance(distance: Float): String {
    return if (distance < METERS_PER_KILOMETER) {
        stringResource(R.string.intake_label_distance_to_site_meters, distance)
    } else {
        stringResource(R.string.intake_label_distance_to_site_kilometers, distance / METERS_PER_KILOMETER)
    }
}

@Composable
private fun CoordinateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun LocationErrorBody(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)) {
        Text(
            text = stringResource(
                R.string.intake_body_location_error,
                state.locationError?.toString(context).orEmpty(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.error,
        )
        ActionButton(
            label = stringResource(R.string.intake_action_retry_location),
            onClick = { onAction(IntakeAction.RetryLocation) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LocationLoadingBody() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colors.secondary,
            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge),
        )
        Text(
            stringResource(R.string.intake_body_getting_location),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.textPrimary,
        )
    }
}
