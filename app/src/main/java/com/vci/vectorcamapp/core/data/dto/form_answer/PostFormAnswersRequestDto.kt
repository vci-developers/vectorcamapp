package com.vci.vectorcamapp.core.data.dto.form_answer

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import com.vci.vectorcamapp.core.data.dto.serializers.FormAnswerValueSerializer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PostFormAnswersRequestDto(
    val answers: List<FormAnswerRequestItemDto> = emptyList(),
    val formVersion: String = "",
    val submittedAt: Long = 0L
)

@Serializable
data class FormAnswerRequestItemDto(
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID = UUID(0, 0),
    val questionId: Int = -1,
    val sessionUnitId: Int? = null,
    @Serializable(with = FormAnswerValueSerializer::class)
    val value: FormAnswerValue = FormAnswerValue.StringValue(""),
    val dataType: String = ""
)
