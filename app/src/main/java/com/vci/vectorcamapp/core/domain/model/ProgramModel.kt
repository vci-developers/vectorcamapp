package com.vci.vectorcamapp.core.domain.model

data class ProgramModel(
    val id: Int,
    val programId: Int,
    val version: String,
    val modelClasses: List<String>,
    val fileSize: Long,
    val fileMd5: String,
    val downloadUrl: String,
    val localFilePath: String,
)
