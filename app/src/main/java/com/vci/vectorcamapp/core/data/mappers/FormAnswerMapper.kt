package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import java.util.UUID

fun FormAnswerEntity.toDomain(): FormAnswer {
    return FormAnswer(
        id = this.id,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}

fun FormAnswer.toEntity(sessionId: UUID, questionId: Int): FormAnswerEntity {
    return FormAnswerEntity(
        id = this.id,
        sessionId = sessionId,
        questionId = questionId,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}
