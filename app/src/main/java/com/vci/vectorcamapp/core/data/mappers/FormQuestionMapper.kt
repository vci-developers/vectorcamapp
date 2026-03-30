package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.form_question.FormQuestionDto
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity
import com.vci.vectorcamapp.core.domain.model.FormQuestion

fun FormQuestionEntity.toDomain(): FormQuestion {
    return FormQuestion(
        id = this.id,
        label = this.label,
        type = this.type,
        required = this.required,
        options = this.options,
        order = this.order
    )
}

fun FormQuestion.toEntity(formId: Int, parentId: Int?): FormQuestionEntity {
    return FormQuestionEntity(
        id = this.id,
        formId = formId,
        parentId = parentId,
        label = this.label,
        type = this.type,
        required = this.required,
        options = this.options,
        order = this.order
    )
}

fun FormQuestionDto.toDomain(): FormQuestion {
    return FormQuestion(
        id = this.id,
        label = this.label,
        type = this.type,
        required = this.required,
        options = this.options,
        order = this.order
    )
}
