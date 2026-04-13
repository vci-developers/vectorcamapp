package com.vci.vectorcamapp.core.data.dto.form

import com.vci.vectorcamapp.core.data.dto.form_question.FormQuestionDto
import kotlinx.serialization.Serializable

@Serializable
data class FormDto(
    val id: Int = -1,
    val programId: Int = -1,
    val name: String = "",
    val version: String = "",
    val questions: List<FormQuestionDto> = emptyList()
)
