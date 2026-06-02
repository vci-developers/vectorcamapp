package com.vci.vectorcamapp.core.data.dto.form_question

import com.vci.vectorcamapp.core.data.dto.serializers.FormQuestionScopeSerializer
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import kotlinx.serialization.Serializable

@Serializable
data class FormQuestionDto(
    val id: Int = -1,
    val formId: Int = -1,
    val parentId: Int? = null,
    val label: String = "",
    val type: String = "",
    val required: Boolean = false,
    val prerequisite: FormQuestionPrerequisiteExpressionDto? = null,
    val options: List<String>? = null,
    val order: Int = -1,
    @Serializable(with = FormQuestionScopeSerializer::class)
    val answerScope: FormQuestionScope = FormQuestionScope.SESSION,
    val isUnitIdentityComponent: Boolean = false,
    val subQuestions: List<FormQuestionDto>? = null
)
