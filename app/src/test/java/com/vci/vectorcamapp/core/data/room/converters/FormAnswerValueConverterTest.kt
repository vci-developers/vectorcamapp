package com.vci.vectorcamapp.core.data.room.converters

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.SerializationException
import org.junit.Test

class FormAnswerValueConverterTest {
    private val converter = FormAnswerValueConverter()

    @Test
    fun stringsRoundTripWithoutLosingTheirTypeOrContents() {
        listOf("", " true ", "123", "[\"Other\"]", "\"quoted\"", "标本\nline").forEach { text ->
            val value = FormAnswerValue.StringValue(text)
            assertThat(converter.toFormAnswerValue(converter.fromFormAnswerValue(value)))
                .isEqualTo(value)
        }
        assertThat(converter.fromFormAnswerValue(FormAnswerValue.StringValue("Other")))
            .isEqualTo("\"Other\"")
    }

    @Test
    fun selectionsRoundTripAsArrays() {
        listOf(emptyList(), listOf("A,B", "\"Other\"", "标本", "line\nbreak")).forEach { selections ->
            val value = FormAnswerValue.ListValue(selections)
            val json = converter.fromFormAnswerValue(value)
            assertThat(json).startsWith("[")
            assertThat(converter.toFormAnswerValue(json)).isEqualTo(value)
        }
    }

    @Test(expected = SerializationException::class)
    fun malformedJsonIsNotSilentlyDiscarded() {
        converter.toFormAnswerValue("not JSON")
    }
}
