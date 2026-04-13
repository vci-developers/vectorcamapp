package com.vci.vectorcamapp.intake.domain.model

sealed interface FormQuestionPrerequisiteValue {
    data class StringValue(val value: String) : FormQuestionPrerequisiteValue
    data class NumberValue(val value: Double) : FormQuestionPrerequisiteValue
    data class BooleanValue(val value: Boolean) : FormQuestionPrerequisiteValue
    data class ListValue(val value: List<FormQuestionPrerequisiteValue>) : FormQuestionPrerequisiteValue
}
