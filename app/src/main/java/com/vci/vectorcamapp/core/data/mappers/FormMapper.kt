package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.form.FormDto
import com.vci.vectorcamapp.core.data.room.entities.FormEntity
import com.vci.vectorcamapp.core.domain.model.Form

fun FormEntity.toDomain(): Form {
    return Form(
        id = this.id,
        name = this.name,
        version = this.version
    )
}

fun Form.toEntity(programId: Int): FormEntity {
    return FormEntity(
        id = this.id,
        programId = programId,
        name = this.name,
        version = this.version
    )
}

fun FormDto.toDomain(): Form {
    return Form(
        id = this.id,
        name = this.name,
        version = this.version
    )
}
