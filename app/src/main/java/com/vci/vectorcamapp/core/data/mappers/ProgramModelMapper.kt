package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.domain.model.ProgramModel

fun ProgramModelDto.toDomain(localFilePath: String): ProgramModel {
    return ProgramModel(
        id = id,
        programId = programId,
        version = version,
        modelClasses = modelClasses,
        fileSize = fileSize,
        fileMd5 = fileMd5,
        downloadUrl = downloadUrl,
        localFilePath = localFilePath,
    )
}
