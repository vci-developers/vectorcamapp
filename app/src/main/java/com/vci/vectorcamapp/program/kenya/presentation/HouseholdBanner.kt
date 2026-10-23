package com.vci.vectorcamapp.program.kenya.presentation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
 
@Composable
fun HouseholdBanner(householdNumber: String?, modifier: Modifier = Modifier) {
    if (householdNumber.isNullOrBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.iconBackground,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_house), contentDescription = null,
                tint = MaterialTheme.colors.textPrimary,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium),
            )
            Text(
                text = stringResource(R.string.kenya_household_number, householdNumber),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}