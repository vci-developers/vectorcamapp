package com.vci.vectorcamapp.core.data.dto.form_answer

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FormAnswerDto(
    val id: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID = UUID(0, 0),
    val sessionId: Int = -1,
    val questionId: Int = -1,
    val value: String = "",
    val dataType: String = "",
    val submittedAt: Long = 0L
)
