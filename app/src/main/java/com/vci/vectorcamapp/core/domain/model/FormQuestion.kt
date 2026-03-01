package com.vci.vectorcamapp.core.domain.model

data class FormQuestion(
    val id: Int,
    val label: String,
    val type: String,
    val required: Boolean,
    val options: List<String>? = null,
    val order: Int? = null
)
