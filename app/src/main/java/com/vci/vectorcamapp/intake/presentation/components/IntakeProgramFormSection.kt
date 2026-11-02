package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun IntakeProgramFormSection(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form ?: return
    IntakeTile(
        title = form.name,
        iconPainter = painterResource(id = R.drawable.ic_clipboard),
        iconDescription = stringResource(R.string.intake_content_description_surveillance_form),
        modifier = modifier,
    ) {
        val answerMap = state.formAnswersByQuestionId.mapValues { (_, answer) -> answer.value }
        state.formQuestions.forEach { question ->
            if (FormQuestionPrerequisiteEvaluator.evaluate(question.prerequisite, answerMap)) {
                DynamicFormField(
                    question = question,
                    value = state.formAnswersByQuestionId[question.id]?.value.orEmpty(),
                    error = state.intakeErrors.formAnswerErrors[question.id],
                    onValueChange = { onAction(IntakeAction.UpdateFormAnswer(question.id, it)) },
                )
            }
        }
    }
}
