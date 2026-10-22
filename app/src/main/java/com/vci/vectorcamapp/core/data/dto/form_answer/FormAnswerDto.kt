package com.vci.vectorcamapp.core.data.dto.form_answer

import com.vci.vectorcamapp.core.data.dto.serializers.FormAnswerValueSerializer
import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FormAnswerDto(
    val id: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID = UUID(0, 0),
    val sessionId: Int = -1,
    val sessionUnitId: Int? = null,
    val questionId: Int = -1,
    @Serializable(with = FormAnswerValueSerializer::class)
    val value: FormAnswerValue = FormAnswerValue.StringValue(""),
    val dataType: String = "",
    val submittedAt: Long = 0L
)
