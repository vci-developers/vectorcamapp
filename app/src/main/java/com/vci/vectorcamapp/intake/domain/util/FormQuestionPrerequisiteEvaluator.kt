package com.vci.vectorcamapp.intake.domain.util

import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue

object FormQuestionPrerequisiteEvaluator {
    fun evaluate(
        expression: FormQuestionPrerequisiteExpression?,
        answers: Map<Int, String>
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
        answers: Map<Int, String>
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
            "empty" -> answer.isBlank()
            "not_empty" -> answer.isNotBlank()
            else -> false
        }
    }

    private fun equalsValue(answer: String, value: FormQuestionPrerequisiteValue?): Boolean {
        return when (value) {
            is FormQuestionPrerequisiteValue.StringValue -> answer == value.value
            is FormQuestionPrerequisiteValue.NumberValue -> answer.toDoubleOrNull() == value.value
            is FormQuestionPrerequisiteValue.BooleanValue -> answer.toBooleanStrictOrNull() == value.value
            else -> false
        }
    }

    private fun compareNumeric(
        answer: String,
        value: FormQuestionPrerequisiteValue?,
        comparison: (Double, Double) -> Boolean
    ): Boolean {
        val answerNum = answer.toDoubleOrNull() ?: return false
        val valueNum = (value as? FormQuestionPrerequisiteValue.NumberValue)?.value ?: return false
        return comparison(answerNum, valueNum)
    }

    private fun isIn(answer: String, value: FormQuestionPrerequisiteValue?): Boolean {
        val list = (value as? FormQuestionPrerequisiteValue.ListValue)?.value ?: return false
        return list.any { equalsValue(answer, it) }
    }

    private fun containsValue(answer: String, value: FormQuestionPrerequisiteValue?): Boolean {
        return when (value) {
            is FormQuestionPrerequisiteValue.StringValue -> answer.contains(value.value)
            else -> false
        }
    }

}