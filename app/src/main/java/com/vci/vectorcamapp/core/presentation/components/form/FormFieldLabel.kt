package com.vci.vectorcamapp.core.presentation.components.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors

@Composable
fun FormFieldLabel(
    text: String,
    required: Boolean,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colors.textSecondary
    val requiredColor = MaterialTheme.colors.error
    val requiredIndicator = stringResource(R.string.core_label_required_indicator)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = labelColor)) { append(text) }
            if (required) {
                withStyle(SpanStyle(color = requiredColor)) { append(requiredIndicator) }
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}
