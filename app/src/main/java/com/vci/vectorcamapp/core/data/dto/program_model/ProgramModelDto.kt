package com.vci.vectorcamapp.core.data.dto.program_model

import kotlinx.serialization.Serializable

@Serializable
data class ProgramModelDto(
    val id: Int = -1,
    val programId: Int = -1,
    val version: String = "",
    val modelClasses: List<String> = emptyList(),
    val fileSize: Long = 0L,
    val fileMd5: String = "",
    val downloadUrl: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
