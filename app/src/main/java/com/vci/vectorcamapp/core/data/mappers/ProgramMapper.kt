package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.program.ProgramDto
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import com.vci.vectorcamapp.core.domain.model.Program

fun ProgramEntity.toDomain(): Program =
    Program(
        id = this.id,
        name = this.name,
        country = this.country,
        formVersion = this.formVersion
    )

fun Program.toEntity(): ProgramEntity =
    ProgramEntity(
        id = this.id,
        name = this.name,
        country = this.country,
        formVersion = this.formVersion
    )

fun ProgramDto.toDomain(): Program {
    return Program(
        id = this.programId,
        name = this.name,
        country = this.country,
        formVersion = this.formVersion
    )
}
