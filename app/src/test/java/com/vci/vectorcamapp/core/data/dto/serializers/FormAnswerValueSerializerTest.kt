package com.vci.vectorcamapp.core.data.dto.serializers

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.form_answer.FormAnswerDto
import com.vci.vectorcamapp.core.data.dto.form_answer.FormAnswerRequestItemDto
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class FormAnswerValueSerializerTest {
    @Test
    fun uploadUsesActualArraysAndDownloadRestoresSelections() {
        val value = FormAnswerValue.ListValue(listOf("A,B", "Other", "标本"))
        val request = FormAnswerRequestItemDto(value = value, dataType = "multi-select")
        val payload = Json.encodeToString(request)
        val array = Json.parseToJsonElement(payload).jsonObject.getValue("value")
        assertThat(array).isInstanceOf(JsonArray::class.java)
        assertThat(Json.decodeFromString<FormAnswerRequestItemDto>(payload)).isEqualTo(request)
        assertThat(Json.decodeFromString<FormAnswerDto>(payload).value).isEqualTo(value)
    }

    @Test
    fun scalarWireFormatRemainsAStringEvenIfItLooksLikeAnArray() {
        val value = FormAnswerValue.StringValue("[\"Other\"]")
        val payload = Json.encodeToString(FormAnswerRequestItemDto(value = value))
        assertThat(Json.parseToJsonElement(payload).jsonObject["value"])
            .isEqualTo(JsonPrimitive("[\"Other\"]"))
        assertThat(Json.decodeFromString<FormAnswerDto>(payload).value).isEqualTo(value)
    }

    @Test
    fun emptySelectionsRemainAnArray() {
        assertThat(Json.encodeToString(FormAnswerValueSerializer, FormAnswerValue.ListValue(emptyList())))
            .isEqualTo("[]")
    }

    @Test(expected = SerializationException::class)
    fun nonStringSelectionsAreRejected() {
        Json.decodeFromString(FormAnswerValueSerializer, "[123]")
    }
}
