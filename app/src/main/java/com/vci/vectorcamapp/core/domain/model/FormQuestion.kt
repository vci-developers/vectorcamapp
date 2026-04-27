package com.vci.vectorcamapp.core.domain.model

import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression

data class FormQuestion(
    val id: Int,
    val label: String,
    val type: String,
    val required: Boolean,
    val prerequisite: FormQuestionPrerequisiteExpression?,
    val options: List<String>?,
    val order: Int?
)
