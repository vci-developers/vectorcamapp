package com.vci.vectorcamapp.core.data.dto.form_question

import kotlinx.serialization.Serializable

@Serializable
data class FormQuestionDto(
    val id: Int = -1,
    val formId: Int = -1,
    val parentId: Int? = null,
    val label: String = "",
    val type: String = "",
    val required: Boolean = false,
    val prerequisite: FormQuestionPrerequisiteExpressionDto? = null,
    val options: List<String>? = null,
    val order: Int = -1,
    val subQuestions: List<FormQuestionDto>? = null
)
