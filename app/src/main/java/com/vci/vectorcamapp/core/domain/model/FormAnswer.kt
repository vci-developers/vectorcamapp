package com.vci.vectorcamapp.core.domain.model

data class FormAnswer(
    val id: Int,
    val sessionId: String,
    val value: String,
    val dataType: String,
    val submittedAt: Long
)
