package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.intake.presentation.components.DynamicFormField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun CollectionBatchFormScreen(
    state: CollectionBatchFormState,
    onAction: (CollectionBatchFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = state.editingUnitId != null

    ScreenHeader(
        title = if (isEditing) "Edit Collection Batch" else "Add Collection Batch",
        subtitle = "Fill out the information below",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(CollectionBatchFormAction.ReturnToPreviousScreen) }
            )
        },
        modifier = modifier,
    ) {
        // Identity questions rendered first
        items(state.identityQuestions, key = { it.id }) { q ->
            DynamicFormField(
                question = q,
                value = state.answers[q.id].orEmpty(),
                onValueChange = { onAction(CollectionBatchFormAction.EnterAnswer(q.id, it)) },
                error = state.errorsByQuestionId[q.id],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimensions.paddingMedium),
            )
        }

        // Other unit questions
        items(state.otherUnitQuestions, key = { it.id }) { q ->
            DynamicFormField(
                question = q,
                value = state.answers[q.id].orEmpty(),
                onValueChange = { onAction(CollectionBatchFormAction.EnterAnswer(q.id, it)) },
                error = state.errorsByQuestionId[q.id],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimensions.paddingMedium),
            )
        }

        // Duplicate identity error banner
        if (state.duplicateIdentityError != null) {
            item {
                Text(
                    text = state.duplicateIdentityError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimensions.paddingMedium),
                )
            }
        }

        item {
            ActionButton(
                label = "Confirm",
                onClick = { onAction(CollectionBatchFormAction.Confirm) },
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimensions.paddingMedium,
                    vertical = MaterialTheme.dimensions.paddingSmall,
                ),
            )
        }
    }
}
