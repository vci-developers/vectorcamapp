package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.domain.model.FormAnswer

fun FormAnswerEntity.toDomain(): FormAnswer {
    return FormAnswer(
        id = this.id,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}

fun FormAnswer.toEntity(sessionId: String, formId: Int, questionId: Int): FormAnswerEntity {
    return FormAnswerEntity(
        id = this.id,
        sessionId = sessionId,
        formId = formId,
        questionId = questionId,
        value = this.value,
        dataType = this.dataType,
        submittedAt = this.submittedAt
    )
}
