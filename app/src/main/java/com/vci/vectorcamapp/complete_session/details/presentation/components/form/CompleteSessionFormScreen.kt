package com.vci.vectorcamapp.complete_session.details.presentation.components.form

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
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompleteSessionFormScreen(
    session: Session,
    site: Site,
    surveillanceForm: SurveillanceForm,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val collectionDateFormatted = dateFormatter.format(Date(session.collectionDate))
    val createdAtDateTimeFormatted = dateTimeFormatter.format(session.createdAt)
    val completedAtDateTimeFormatted = session.completedAt?.let { dateTimeFormatter.format(it) }
    val submittedAtDateTimeFormatted = session.submittedAt?.let { dateTimeFormatter.format(it) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Session ID: ${session.localId}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Created At: $createdAtDateTimeFormatted",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Completed At: $completedAtDateTimeFormatted",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Submitted At: $submittedAtDateTimeFormatted",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Site ID: ${site.id}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sentinel Site: ${site.sentinelSite}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Health Center: ${site.healthCenter}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "District: ${site.district}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sub-County: ${site.subCounty}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Parish: ${site.parish}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collector: ${session.collectorName} (${session.collectorTitle})",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collection Date: $collectionDateFormatted",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Collection Method: ${session.collectionMethod}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Specimen Condition: ${session.specimenCondition}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Notes: ${session.notes}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Number of People Slept in House: ${surveillanceForm.numPeopleSleptInHouse}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "IRS Conducted: ${surveillanceForm.wasIrsConducted}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Months Since IRS: ${surveillanceForm.monthsSinceIrs}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Number of LLINs Available: ${surveillanceForm.numLlinsAvailable}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "LLIN Type: ${surveillanceForm.llinType}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "LLIN Brand: ${surveillanceForm.llinBrand}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Number of People Slept Under LLIN: ${surveillanceForm.numPeopleSleptUnderLlin}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
