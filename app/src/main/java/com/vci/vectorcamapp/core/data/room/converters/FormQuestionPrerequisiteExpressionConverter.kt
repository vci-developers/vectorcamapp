package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FormQuestionPrerequisiteExpressionConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    @TypeConverter
    fun fromPrerequisiteExpression(value: FormQuestionPrerequisiteExpression?): String? {
        if (value == null) return null
        return runCatching {
            val element = expressionToJson(value)
            json.encodeToString(JsonElement.serializer(), element)
        }.getOrNull()
    }

    @TypeConverter
    fun toPrerequisiteExpression(value: String?): FormQuestionPrerequisiteExpression? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val element = json.decodeFromString(JsonElement.serializer(), value)
            jsonToExpression(element.jsonObject)
        }.getOrNull()
    }

    private fun expressionToJson(expression: FormQuestionPrerequisiteExpression): JsonObject {
        return when (expression) {
            is FormQuestionPrerequisiteExpression.Predicate -> buildMap {
                put("questionId", JsonPrimitive(expression.questionId))
                put("operator", JsonPrimitive(expression.operator))
                expression.value?.let { put("value", valueToJson(it)) }
            }.let { JsonObject(it) }

            is FormQuestionPrerequisiteExpression.All -> JsonObject(
                mapOf("all" to JsonArray(expression.expressions.map { expressionToJson(it) }))
            )

            is FormQuestionPrerequisiteExpression.Any -> JsonObject(
                mapOf("any" to JsonArray(expression.expressions.map { expressionToJson(it) }))
            )

            is FormQuestionPrerequisiteExpression.Not -> JsonObject(
                mapOf("not" to expressionToJson(expression.expression))
            )
        }
    }

    private fun valueToJson(value: FormQuestionPrerequisiteValue): JsonElement {
        return when (value) {
            is FormQuestionPrerequisiteValue.StringValue -> JsonPrimitive(value.value)
            is FormQuestionPrerequisiteValue.NumberValue -> JsonPrimitive(value.value)
            is FormQuestionPrerequisiteValue.BooleanValue -> JsonPrimitive(value.value)
            is FormQuestionPrerequisiteValue.ListValue -> JsonArray(value.value.map { valueToJson(it) })
        }
    }

    private fun jsonToExpression(obj: JsonObject): FormQuestionPrerequisiteExpression {
        return when {
            "questionId" in obj -> {
                val questionId = obj.getValue("questionId").jsonPrimitive.int
                val operator = obj.getValue("operator").jsonPrimitive.content
                val value = obj["value"]?.let { jsonToValue(it) }
                FormQuestionPrerequisiteExpression.Predicate(questionId, operator, value)
            }

            "all" in obj -> FormQuestionPrerequisiteExpression.All(
                expressions = obj.getValue("all").jsonArray.map { jsonToExpression(it.jsonObject) }
            )

            "any" in obj -> FormQuestionPrerequisiteExpression.Any(
                expressions = obj.getValue("any").jsonArray.map { jsonToExpression(it.jsonObject) }
            )

            "not" in obj -> FormQuestionPrerequisiteExpression.Not(
                expression = jsonToExpression(obj.getValue("not").jsonObject)
            )

            else -> throw IllegalArgumentException("Invalid prerequisite JSON: $obj")
        }
    }

    private fun jsonToValue(element: JsonElement): FormQuestionPrerequisiteValue {
        return when (element) {
            is JsonArray -> FormQuestionPrerequisiteValue.ListValue(
                value = element.map { jsonToValue(it) }
            )

            is JsonPrimitive -> {
                element.booleanOrNull?.let { return FormQuestionPrerequisiteValue.BooleanValue(it) }
                element.doubleOrNull?.let { return FormQuestionPrerequisiteValue.NumberValue(it) }
                FormQuestionPrerequisiteValue.StringValue(element.content)
            }

            else -> FormQuestionPrerequisiteValue.StringValue(element.toString())
        }
    }
}