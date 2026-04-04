package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.form_question.FormQuestionDto
import com.vci.vectorcamapp.core.data.dto.form_question.FormQuestionPrerequisiteExpressionDto
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

fun FormQuestionEntity.toDomain(): FormQuestion {
    return FormQuestion(
        id = this.id,
        label = this.label,
        type = this.type,
        required = this.required,
        prerequisite = this.prerequisite,
        options = this.options,
        order = this.order
    )
}

fun FormQuestion.toEntity(formId: Int, parentId: Int?): FormQuestionEntity {
    return FormQuestionEntity(
        id = this.id,
        formId = formId,
        parentId = parentId,
        label = this.label,
        type = this.type,
        required = this.required,
        prerequisite = this.prerequisite,
        options = this.options,
        order = this.order
    )
}

fun FormQuestionDto.toDomain(): FormQuestion {
    return FormQuestion(
        id = this.id,
        label = this.label,
        type = this.type,
        required = this.required,
        prerequisite = this.prerequisite?.toDomain(),
        options = this.options,
        order = this.order
    )
}

private fun FormQuestionPrerequisiteExpressionDto.toDomain(): FormQuestionPrerequisiteExpression {
    return when {
        questionId != null && operator != null -> FormQuestionPrerequisiteExpression.Predicate(
            questionId = questionId,
            operator = operator,
            value = value?.toPrerequisiteValue()
        )

        all != null -> FormQuestionPrerequisiteExpression.All(
            expressions = all.map { it.toDomain() }
        )

        any != null -> FormQuestionPrerequisiteExpression.Any(
            expressions = any.map { it.toDomain() }
        )

        not != null -> FormQuestionPrerequisiteExpression.Not(
            expression = not.toDomain()
        )

        else -> throw IllegalArgumentException("Invalid prerequisite expression: $this")
    }
}

private fun JsonElement.toPrerequisiteValue(): FormQuestionPrerequisiteValue {
    return when (this) {
        is JsonArray -> FormQuestionPrerequisiteValue.ListValue(
            value = map { it.toPrerequisiteValue() }
        )

        is JsonPrimitive -> {
            booleanOrNull?.let { return FormQuestionPrerequisiteValue.BooleanValue(it) }
            doubleOrNull?.let { return FormQuestionPrerequisiteValue.NumberValue(it) }
            FormQuestionPrerequisiteValue.StringValue(content)
        }

        else -> FormQuestionPrerequisiteValue.StringValue(toString())
    }
}

