package com.vci.vectorcamapp.complete_session.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompleteSessionDetailsScreen(
    state: CompleteSessionDetailsState,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val formattedDateTime = dateTimeFormatter.format(state.session.createdAt)
    val collectionDateFormatted = if (state.surveillanceForm.collectionDate != 0L)
        dateFormatter.format(Date(state.surveillanceForm.collectionDate))
    else "N/A"

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Session ID: ${state.session.id}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Created: $formattedDateTime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Country: ${state.surveillanceForm.country}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "District: ${state.surveillanceForm.district}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Health Center: ${state.surveillanceForm.healthCenter}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sentinel Site: ${state.surveillanceForm.sentinelSite}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Household Number: ${state.surveillanceForm.householdNumber}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Longitude: ${state.surveillanceForm.longitude}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Latitude: ${state.surveillanceForm.latitude}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collection Method: ${state.surveillanceForm.collectionMethod}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collection Date: $collectionDateFormatted",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collector: ${state.surveillanceForm.collectorName} (${state.surveillanceForm.collectorTitle})",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Number of People Slept in House: ${state.surveillanceForm.numPeopleSleptInHouse}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "IRS Conducted: ${if (state.surveillanceForm.wasIrsConducted) "Yes" else "No"}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Months Since IRS: ${state.surveillanceForm.monthsSinceIrs ?: "N/A"}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Number of LLIs Available: ${state.surveillanceForm.numLlinsAvailable}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
