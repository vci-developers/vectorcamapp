package com.vci.vectorcamapp.intake.domain.util

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import org.junit.Test

class FormQuestionPrerequisiteEvaluatorTest {

    @Test
    fun nullExpression_isAlwaysTrue() {
        assertThat(FormQuestionPrerequisiteEvaluator.evaluate(null, emptyMap())).isTrue()
    }

    @Test
    fun all_requiresEveryChild() {
        val expression = FormQuestionPrerequisiteExpression.All(
            listOf(eq(1, "yes"), eq(2, "ok"))
        )

        assertThat(evaluate(expression, 1 to "yes", 2 to "ok")).isTrue()
        assertThat(evaluate(expression, 1 to "yes", 2 to "no")).isFalse()
    }

    @Test
    fun any_requiresOneChild() {
        val expression = FormQuestionPrerequisiteExpression.Any(
            listOf(eq(1, "yes"), eq(2, "ok"))
        )

        assertThat(evaluate(expression, 1 to "no", 2 to "ok")).isTrue()
        assertThat(evaluate(expression, 1 to "no", 2 to "no")).isFalse()
    }

    @Test
    fun not_invertsChild() {
        val expression = FormQuestionPrerequisiteExpression.Not(eq(1, "yes"))

        assertThat(evaluate(expression, 1 to "no")).isTrue()
        assertThat(evaluate(expression, 1 to "yes")).isFalse()
    }

    @Test
    fun missingAnswer_failsPredicate() {
        val expression = eq(1, "yes")
        assertThat(FormQuestionPrerequisiteEvaluator.evaluate(expression, emptyMap())).isFalse()
    }

    @Test
    fun equality_matchesStringNumberAndBoolean() {
        assertThat(evaluate(eq(1, "village"), 1 to "village")).isTrue()
        assertThat(evaluate(eq(1, "village"), 1 to "town")).isFalse()

        assertThat(
            evaluate(predicate(1, "eq", FormQuestionPrerequisiteValue.NumberValue(2.5)), 1 to "2.5")
        ).isTrue()
        assertThat(
            evaluate(predicate(1, "eq", FormQuestionPrerequisiteValue.NumberValue(2.5)), 1 to "nope")
        ).isFalse()

        assertThat(
            evaluate(predicate(1, "eq", FormQuestionPrerequisiteValue.BooleanValue(true)), 1 to "true")
        ).isTrue()
        assertThat(
            evaluate(predicate(1, "eq", FormQuestionPrerequisiteValue.BooleanValue(true)), 1 to "TRUE")
        ).isFalse()

        assertThat(
            evaluate(predicate(1, "eq", FormQuestionPrerequisiteValue.ListValue(emptyList())), 1 to "x")
        ).isFalse()
        assertThat(evaluate(predicate(1, "eq", null), 1 to "x")).isFalse()
    }

    @Test
    fun inequality_invertsEquality() {
        assertThat(evaluate(predicate(1, "neq", FormQuestionPrerequisiteValue.StringValue("a")), 1 to "b")).isTrue()
        assertThat(evaluate(predicate(1, "neq", FormQuestionPrerequisiteValue.StringValue("a")), 1 to "a")).isFalse()
    }

    @Test
    fun numericComparisons_coverEachOperatorAndInvalidInput() {
        assertThat(evaluate(numberOp("gt", 3.0), 1 to "4")).isTrue()
        assertThat(evaluate(numberOp("gt", 3.0), 1 to "3")).isFalse()
        assertThat(evaluate(numberOp("gte", 3.0), 1 to "3")).isTrue()
        assertThat(evaluate(numberOp("lt", 3.0), 1 to "2")).isTrue()
        assertThat(evaluate(numberOp("lte", 3.0), 1 to "3")).isTrue()
        assertThat(evaluate(numberOp("gt", 3.0), 1 to "abc")).isFalse()
        assertThat(
            evaluate(predicate(1, "gt", FormQuestionPrerequisiteValue.StringValue("3")), 1 to "4")
        ).isFalse()
    }

    @Test
    fun membership_matchesAnyListItem() {
        val list = FormQuestionPrerequisiteValue.ListValue(
            listOf(
                FormQuestionPrerequisiteValue.StringValue("a"),
                FormQuestionPrerequisiteValue.NumberValue(2.0),
            )
        )

        assertThat(evaluate(predicate(1, "in", list), 1 to "a")).isTrue()
        assertThat(evaluate(predicate(1, "in", list), 1 to "2")).isTrue()
        assertThat(evaluate(predicate(1, "in", list), 1 to "z")).isFalse()
        assertThat(evaluate(predicate(1, "not_in", list), 1 to "z")).isTrue()
        assertThat(evaluate(predicate(1, "not_in", list), 1 to "a")).isFalse()
        assertThat(
            evaluate(predicate(1, "in", FormQuestionPrerequisiteValue.StringValue("a")), 1 to "a")
        ).isFalse()
    }

    @Test
    fun contains_matchesSubstringOnlyForStrings() {
        assertThat(
            evaluate(predicate(1, "contains", FormQuestionPrerequisiteValue.StringValue("ill")), 1 to "village")
        ).isTrue()
        assertThat(
            evaluate(predicate(1, "contains", FormQuestionPrerequisiteValue.StringValue("town")), 1 to "village")
        ).isFalse()
        assertThat(
            evaluate(predicate(1, "not_contains", FormQuestionPrerequisiteValue.StringValue("town")), 1 to "village")
        ).isTrue()
        assertThat(
            evaluate(predicate(1, "contains", FormQuestionPrerequisiteValue.NumberValue(1.0)), 1 to "1")
        ).isFalse()
    }

    @Test
    fun emptiness_andUnknownOperator() {
        assertThat(evaluate(predicate(1, "empty", null), 1 to "   ")).isTrue()
        assertThat(evaluate(predicate(1, "empty", null), 1 to "x")).isFalse()
        assertThat(evaluate(predicate(1, "not_empty", null), 1 to "x")).isTrue()
        assertThat(evaluate(predicate(1, "not_empty", null), 1 to "")).isFalse()
        assertThat(evaluate(predicate(1, "starts_with", null), 1 to "x")).isFalse()
    }

    private fun evaluate(
        expression: FormQuestionPrerequisiteExpression,
        vararg answers: Pair<Int, String>,
    ): Boolean = FormQuestionPrerequisiteEvaluator.evaluate(expression, answers.toMap())

    private fun eq(questionId: Int, value: String) =
        predicate(questionId, "eq", FormQuestionPrerequisiteValue.StringValue(value))

    private fun numberOp(operator: String, value: Double) =
        predicate(1, operator, FormQuestionPrerequisiteValue.NumberValue(value))

    private fun predicate(
        questionId: Int,
        operator: String,
        value: FormQuestionPrerequisiteValue?,
    ) = FormQuestionPrerequisiteExpression.Predicate(questionId, operator, value)
}
