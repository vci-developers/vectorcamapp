package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.collection_batch.form.presentation.components.CollectionBatchFormTile
import com.vci.vectorcamapp.collection_batch.form.presentation.components.DuplicateIdentityWarningBanner
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
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
        title = stringResource(R.string.collection_batch_title_form),
        subtitle = stringResource(R.string.collection_batch_body_form_subtitle),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.collection_batch_content_description_back),
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
                    title = stringResource(R.string.collection_batch_title_identity),
                    iconPainter = painterResource(R.drawable.ic_info),
                    iconDescription = stringResource(R.string.collection_batch_content_description_identity),
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
                                error = state.collectionBatchFormErrors.formAnswerErrors[question.id],
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (state.collectionBatchFormErrors.duplicateIdentity != null) {
                        DuplicateIdentityWarningBanner(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (otherQuestions.isNotEmpty()) {
            item {
                CollectionBatchFormTile(
                    title = stringResource(R.string.collection_batch_title_information),
                    iconPainter = painterResource(R.drawable.ic_clipboard),
                    iconDescription = stringResource(R.string.collection_batch_content_description_information),
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
                                error = state.collectionBatchFormErrors.formAnswerErrors[question.id],
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        item {
            ActionButton(
                label = stringResource(R.string.collection_batch_action_continue),
                onClick = { onAction(CollectionBatchFormAction.SubmitSessionUnitForm) },
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimensions.paddingMedium,
                    vertical = MaterialTheme.dimensions.paddingSmall,
                ),
            )
        }

    }
}
