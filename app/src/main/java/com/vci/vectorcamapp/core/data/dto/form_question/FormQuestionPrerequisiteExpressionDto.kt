package com.vci.vectorcamapp.core.data.dto.form_question

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FormQuestionPrerequisiteExpressionDto(
    val questionId: Int? = null,
    val operator: String? = null,
    val value: JsonElement? = null,
    val all: List<FormQuestionPrerequisiteExpressionDto>? = null,
    val any: List<FormQuestionPrerequisiteExpressionDto>? = null,
    val not: FormQuestionPrerequisiteExpressionDto? = null
)
