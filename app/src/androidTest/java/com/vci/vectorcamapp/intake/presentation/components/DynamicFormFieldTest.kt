package com.vci.vectorcamapp.intake.presentation.components
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
class DynamicFormFieldTest {
    @get:Rule
    val compose = createComposeRule()
    @Test
    fun batchQrQuestionShowsEditableInputAndScannerButton() {
        var answer: FormAnswerValue = FormAnswerValue.StringValue("")
        val scanLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.core_title_scan_qr_code)
        compose.setContent {
            VectorcamappTheme {
                DynamicFormField(
                    question = question(1, "qr", "Scan cup ID for INDOORS")
                        .copy(answerScope = FormQuestionScope.SESSION_UNIT),
                    value = answer,
                    onValueChange = { answer = it },
                )
            }
        }
        compose.onNodeWithText("Scan cup ID for INDOORS").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertIsDisplayed().performTextInput("cup-123")
        compose.onNodeWithContentDescription(scanLabel).assertIsDisplayed().assertHasClickAction()
        compose.runOnIdle { assertThat(answer).isEqualTo(FormAnswerValue.StringValue("cup-123")) }
    }
    @Test
    fun textQuestionRendersAndAcceptsTypedInput() {
        var answer: FormAnswerValue = FormAnswerValue.StringValue("")
        compose.setContent {
            VectorcamappTheme {
                DynamicFormField(
                    question = question(1, "text", "Village"),
                    value = answer,
                    onValueChange = { answer = it },
                )
            }
        }
        compose.onNodeWithText("Village").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("Kampala")
        compose.runOnIdle { assertThat(answer).isEqualTo(FormAnswerValue.StringValue("Kampala")) }
    }
    @Test
    fun selectingOtherShowsFollowUpAndDeselectingHidesIt() {
        val prerequisite = FormQuestionPrerequisiteExpression.Predicate(
            questionId = 1,
            operator = "contains",
            value = FormQuestionPrerequisiteValue.StringValue("Other"),
        )
        compose.setContent {
            var answer by remember { mutableStateOf<FormAnswerValue>(FormAnswerValue.ListValue(emptyList())) }
            VectorcamappTheme {
                Column {
                    DynamicFormField(
                        question = question(1, "multi-select", "Animals").copy(options = listOf("Cattle", "Other")),
                        value = answer,
                        onValueChange = { answer = it },
                    )
                    if (FormQuestionPrerequisiteEvaluator.evaluate(prerequisite, mapOf(1 to answer))) {
                        DynamicFormField(
                            question = question(2, "text", "Other animals"),
                            value = FormAnswerValue.StringValue(""),
                            onValueChange = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Animals").assertIsDisplayed()
        compose.onNodeWithText("Other animals").assertDoesNotExist()
        compose.onNodeWithText("Other").performClick()
        compose.onNodeWithText("Other animals").assertIsDisplayed()
        compose.onNodeWithText("Other").performClick()
        compose.onNodeWithText("Other animals").assertDoesNotExist()
    }
    private fun question(id: Int, type: String, label: String) = FormQuestion(
        id = id,
        label = label,
        type = type,
        required = false,
        prerequisite = null,
        options = null,
        order = id,
        answerScope = FormQuestionScope.SESSION,
        isUnitIdentityComponent = false,
    )
}