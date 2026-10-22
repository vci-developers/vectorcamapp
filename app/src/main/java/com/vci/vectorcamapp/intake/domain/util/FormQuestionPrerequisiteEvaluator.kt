package com.vci.vectorcamapp.intake.domain.util

import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue

object FormQuestionPrerequisiteEvaluator {
    fun evaluate(
        expression: FormQuestionPrerequisiteExpression?,
        answers: Map<Int, FormAnswerValue>
    ): Boolean {
        if (expression == null) return true

        return when (expression) {
            is FormQuestionPrerequisiteExpression.All ->
                expression.expressions.all { evaluate(it, answers) }

            is FormQuestionPrerequisiteExpression.Any ->
                expression.expressions.any { evaluate(it, answers) }

            is FormQuestionPrerequisiteExpression.Not ->
                !evaluate(expression.expression, answers)

            is FormQuestionPrerequisiteExpression.Predicate ->
                evaluatePredicate(expression, answers)
        }
    }

    private fun evaluatePredicate(
        predicate: FormQuestionPrerequisiteExpression.Predicate,
        answers: Map<Int, FormAnswerValue>
    ): Boolean {
        val answer = answers[predicate.questionId] ?: return false
        val value = predicate.value

        return when (predicate.operator) {
            "eq" -> equalsValue(answer, value)
            "neq" -> !equalsValue(answer, value)
            "gt" -> compareNumeric(answer, value) { a, b -> a > b }
            "gte" -> compareNumeric(answer, value) { a, b -> a >= b }
            "lt" -> compareNumeric(answer, value) { a, b -> a < b }
            "lte" -> compareNumeric(answer, value) { a, b -> a <= b }
            "in" -> isIn(answer, value)
            "not_in" -> !isIn(answer, value)
            "contains" -> containsValue(answer, value)
            "not_contains" -> !containsValue(answer, value)
            "empty" -> answer.isEmpty
            "not_empty" -> !answer.isEmpty
            else -> false
        }
    }

    private fun equalsValue(answer: FormAnswerValue, value: FormQuestionPrerequisiteValue?): Boolean {
        if (answer is FormAnswerValue.ListValue) {
            val expected = (value as? FormQuestionPrerequisiteValue.ListValue)?.value ?: return false
            val strings = expected.map {
                (it as? FormQuestionPrerequisiteValue.StringValue)?.value ?: return false
            }
            return answer.value.toSet() == strings.toSet()
        }
        val text = (answer as FormAnswerValue.StringValue).value
        return when (value) {
            is FormQuestionPrerequisiteValue.StringValue -> text == value.value
            is FormQuestionPrerequisiteValue.NumberValue -> text.toDoubleOrNull() == value.value
            is FormQuestionPrerequisiteValue.BooleanValue -> text.toBooleanStrictOrNull() == value.value
            else -> false
        }
    }

    private fun compareNumeric(
        answer: FormAnswerValue,
        value: FormQuestionPrerequisiteValue?,
        comparison: (Double, Double) -> Boolean
    ): Boolean {
        val answerNum = (answer as? FormAnswerValue.StringValue)?.value?.toDoubleOrNull() ?: return false
        val valueNum = (value as? FormQuestionPrerequisiteValue.NumberValue)?.value ?: return false
        return comparison(answerNum, valueNum)
    }

    private fun isIn(answer: FormAnswerValue, value: FormQuestionPrerequisiteValue?): Boolean {
        val list = (value as? FormQuestionPrerequisiteValue.ListValue)?.value ?: return false
        return list.any { equalsValue(answer, it) }
    }

    private fun containsValue(answer: FormAnswerValue, value: FormQuestionPrerequisiteValue?): Boolean {
        return when (value) {
            is FormQuestionPrerequisiteValue.StringValue -> when (answer) {
                is FormAnswerValue.StringValue -> answer.value.contains(value.value)
                is FormAnswerValue.ListValue -> value.value in answer.value
            }
            else -> false
        }
    }

}
