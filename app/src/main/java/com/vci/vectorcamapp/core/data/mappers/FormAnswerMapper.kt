package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import java.util.UUID

fun FormAnswerEntity.toDomain(): FormAnswer {
    return FormAnswer(
        localId = this.localId,
        remoteId = this.remoteId,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}

fun FormAnswer.toEntity(sessionId: UUID, sessionUnitId: UUID?, questionId: Int): FormAnswerEntity {
    return FormAnswerEntity(
        localId = this.localId,
        remoteId = this.remoteId,
        sessionId = sessionId,
        sessionUnitId = sessionUnitId,
        questionId = questionId,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}
