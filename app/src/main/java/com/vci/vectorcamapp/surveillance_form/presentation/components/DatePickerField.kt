package com.vci.vectorcamapp.surveillance_form.presentation.components

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    selectedDateInMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()) }
    calendar.timeInMillis = selectedDateInMillis

    val formattedDate = remember(selectedDateInMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDateInMillis)
    }

    OutlinedTextField(
        value = formattedDate,
        onValueChange = { },
        label = { Text(label) },
        enabled = false,
        singleLine = true,
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = "Calendar"
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        onDateSelected(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
    )
}
