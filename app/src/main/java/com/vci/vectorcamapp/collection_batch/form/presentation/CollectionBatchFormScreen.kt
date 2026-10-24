package com.vci.vectorcamapp.collection_batch.form.presentation
import com.vci.vectorcamapp.program.kenya.presentation.HouseholdBanner
import com.vci.vectorcamapp.program.kenya.domain.KenyaFormRules
import com.vci.vectorcamapp.core.domain.model.FormQuestion

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
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
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
        item {
            HouseholdBanner(
                state.householdNumber,
                Modifier.padding(horizontal = MaterialTheme.dimensions.paddingMedium)
            )
        }
        if (identityQuestions.isNotEmpty()) {
            item {
                CollectionBatchFormTile(
                    title = stringResource(R.string.collection_batch_title_identity),
                    iconPainter = painterResource(R.drawable.ic_info),
                    iconDescription = stringResource(R.string.collection_batch_content_description_identity),
                ) {
                    identityQuestions.forEach { question ->
                        if (FormQuestionPrerequisiteEvaluator.evaluate(
                                question.prerequisite,
                                answerMap,
                            )
                        ) {
                            DynamicFormField(
                                question = question,
                                value = state.formAnswersByQuestionId.getValue(question.id).value,
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
                    state.formQuestions.filter {
                        state.expectedCupCodesByQuestionId.containsKey(it.id) &&
                            state.expectedCupCodesByQuestionId[it.id] == null
                    }.forEach { question ->
                        DuplicateIdentityWarningBanner(
                            modifier = Modifier.fillMaxWidth(),
                            message = missingCupMessage(question),
                        )
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
                                question.prerequisite,
                                answerMap,
                            )
                        ) {
                            DynamicFormField(
                                question = question,
                                value = state.formAnswersByQuestionId.getValue(question.id).value,
                                onValueChange = {
                                    onAction(
                                        CollectionBatchFormAction.UpdateFormAnswer(question.id, it)
                                    )
                                },
                                error = state.collectionBatchFormErrors.formAnswerErrors[question.id],
                                errorMessage = if (state.collectionBatchFormErrors.formAnswerErrors[question.id] ==
                                    CollectionBatchFormError.CUP_NOT_RECORDED) {
                                    missingCupMessage(question)
                                } else state.expectedCupCodesByQuestionId[question.id]?.takeIf {
                                    state.collectionBatchFormErrors.formAnswerErrors[question.id] ==
                                        CollectionBatchFormError.CUP_CODE_MISMATCH
                                }?.let { stringResource(R.string.collection_batch_form_error_recorded_cup_code, it) },
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
@Composable
private fun missingCupMessage(question: FormQuestion): String = stringResource(
    when (KenyaFormRules.cupLocationToken(question)) {
        "IND" -> R.string.collection_batch_form_error_cup_not_recorded_indoors
        "OUT" -> R.string.collection_batch_form_error_cup_not_recorded_outdoors
        "ANS" -> R.string.collection_batch_form_error_cup_not_recorded_animal_shelter
        else -> R.string.collection_batch_form_error_cup_not_recorded
    }
)
