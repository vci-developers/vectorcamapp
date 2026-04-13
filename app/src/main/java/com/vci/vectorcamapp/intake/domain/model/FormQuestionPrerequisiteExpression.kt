package com.vci.vectorcamapp.intake.domain.model

sealed interface FormQuestionPrerequisiteExpression {
    data class Predicate(
        val questionId: Int,
        val operator: String,
        val value: FormQuestionPrerequisiteValue?
    ) : FormQuestionPrerequisiteExpression

    data class All(val expressions: List<FormQuestionPrerequisiteExpression>) : FormQuestionPrerequisiteExpression
    data class Any(val expressions: List<FormQuestionPrerequisiteExpression>) : FormQuestionPrerequisiteExpression
    data class Not(val expression: FormQuestionPrerequisiteExpression) : FormQuestionPrerequisiteExpression
}
