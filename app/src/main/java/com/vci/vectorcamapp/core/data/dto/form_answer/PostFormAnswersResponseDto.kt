package com.vci.vectorcamapp.core.data.dto.form_answer

import kotlinx.serialization.Serializable

@Serializable
data class PostFormAnswersResponseDto(
    val message: String = "",
    val answers: List<FormAnswerDto> = emptyList()
)
