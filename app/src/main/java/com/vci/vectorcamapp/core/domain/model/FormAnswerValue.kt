package com.vci.vectorcamapp.core.domain.model

sealed interface FormAnswerValue {
    data class StringValue(val value: String) : FormAnswerValue
    data class ListValue(val value: List<String>) : FormAnswerValue

    val isEmpty: Boolean
        get() = when (this) {
            is StringValue -> value.isBlank()
            is ListValue -> value.isEmpty()
        }

    fun displayText(): String = when (this) {
        is StringValue -> value
        is ListValue -> value.joinToString(", ")
    }

    fun identityValue(): FormAnswerValue = when (this) {
        is StringValue -> copy(value = value.trim())
        is ListValue -> copy(value = value.distinct().sorted())
    }

    companion object {
        fun emptyFor(type: String): FormAnswerValue = when (type) {
            "multi-select" -> ListValue(emptyList())
            "boolean" -> StringValue("false")
            else -> StringValue("")
        }
    }
}
