package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.program.ProgramConfigDto
import com.vci.vectorcamapp.core.data.dto.program.ProgramDto
import com.vci.vectorcamapp.core.data.dto.program.ProgramModelsConfigDto
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig

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

fun ProgramModelsConfigDto.toDomain(): ProgramModelsConfig {
    return ProgramModelsConfig(
        species = species,
        sex = sex,
        abdomenStatus = abdomenStatus,
        detect = detect,
    )
}

fun ProgramConfigDto.toModelsConfig(): ProgramModelsConfig? {
    return models?.toDomain()
}

fun ProgramModelDto.toDomain(localFilePath: String): ProgramModel {
    return ProgramModel(
        id = id,
        programId = programId,
        modelId = modelId,
        modelClasses = modelClasses,
        fileSize = fileSize,
        fileMd5 = fileMd5,
        downloadUrl = downloadUrl,
        localFilePath = localFilePath,
    )
}
