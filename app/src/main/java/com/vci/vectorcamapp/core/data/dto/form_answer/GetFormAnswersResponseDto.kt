package com.vci.vectorcamapp.core.data.dto.form_answer

import kotlinx.serialization.Serializable

@Serializable
data class GetFormAnswersResponseDto(
    val formId: Int = -1,
    val formName: String = "",
    val formVersion: String = "",
    val programId: Int = -1,
    val sessionId: Int = -1,
    val answers: List<FormAnswerDto> = emptyList()
)
