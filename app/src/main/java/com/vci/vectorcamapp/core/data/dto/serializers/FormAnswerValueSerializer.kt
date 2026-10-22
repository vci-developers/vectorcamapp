package com.vci.vectorcamapp.core.data.dto.serializers

import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

/** The API represents answers as strings or arrays, without a type wrapper. */
object FormAnswerValueSerializer : KSerializer<FormAnswerValue> {
    override val descriptor = buildClassSerialDescriptor("FormAnswerValue")

    override fun serialize(encoder: Encoder, value: FormAnswerValue) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(
            when (value) {
                is FormAnswerValue.StringValue -> JsonPrimitive(value.value)
                is FormAnswerValue.ListValue -> JsonArray(value.value.map(::JsonPrimitive))
            }
        )
    }

    override fun deserialize(decoder: Decoder): FormAnswerValue {
        val jsonDecoder = decoder as JsonDecoder
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) throw SerializationException("Expected an answer string")
                FormAnswerValue.StringValue(element.content)
            }
            is JsonArray -> FormAnswerValue.ListValue(element.map { item ->
                val text = item as? JsonPrimitive
                if (text == null || !text.isString) {
                    throw SerializationException("Expected string selections")
                }
                text.content
            })
            else -> throw SerializationException("Expected an answer string or string array")
        }
    }
}
