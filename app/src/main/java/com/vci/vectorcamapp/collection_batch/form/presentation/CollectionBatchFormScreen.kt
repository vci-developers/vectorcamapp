package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.collection_batch.form.presentation.components.CollectionBatchFormTile
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.intake.presentation.components.DynamicFormField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun CollectionBatchFormScreen(
    state: CollectionBatchFormState,
    onAction: (CollectionBatchFormAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val (identityQuestions, otherQuestions) =
        state.formQuestions.partition { it.isUnitIdentityComponent }

    val answerMap = state.formAnswersByQuestionId.mapValues { (_, answer) -> answer.value }

    ScreenHeader(
        title = "Collection Batch Form",
        subtitle = "Fill out the information below",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(CollectionBatchFormAction.ReturnToCollectionBatchListScreen) }
            )
        },
        modifier = modifier
    ) {
        if (identityQuestions.isNotEmpty()) {
            item {
                CollectionBatchFormTile(
                    title = "Batch Identity",
                    iconPainter = painterResource(R.drawable.ic_info),
                    iconDescription = "Batch Identity Icon",
                ) {
                    identityQuestions.forEach { question ->
                        if (FormQuestionPrerequisiteEvaluator.evaluate(
                                question.prerequisite, answerMap,
                            )
                        ) {
                            DynamicFormField(
                                question = question,
                                value = state.formAnswersByQuestionId[question.id]?.value.orEmpty(),
                                onValueChange = {
                                    onAction(
                                        CollectionBatchFormAction.UpdateFormAnswer(question.id, it)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        if (otherQuestions.isNotEmpty()) {
            item {
                CollectionBatchFormTile(
                    title = "Batch Information",
                    iconPainter = painterResource(R.drawable.ic_clipboard),
                    iconDescription = "Batch Information Icon",
                ) {
                    otherQuestions.forEach { question ->
                        if (FormQuestionPrerequisiteEvaluator.evaluate(
                                question.prerequisite, answerMap,
                            )
                        ) {
                            DynamicFormField(
                                question = question,
                                value = state.formAnswersByQuestionId[question.id]?.value.orEmpty(),
                                onValueChange = {
                                    onAction(
                                        CollectionBatchFormAction.UpdateFormAnswer(question.id, it)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
