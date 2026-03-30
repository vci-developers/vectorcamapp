package com.vci.vectorcamapp.core.data.dto.form_answer

import kotlinx.serialization.Serializable

@Serializable
data class FormAnswerDto(
    val id: Int = -1,
    val sessionId: Int = -1,
    val questionId: Int = -1,
    val value: String = "",
    val dataType: String = "",
    val submittedAt: Long = 0L
)
