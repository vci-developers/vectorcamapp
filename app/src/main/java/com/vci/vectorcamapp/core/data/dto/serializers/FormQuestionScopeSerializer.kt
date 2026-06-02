package com.vci.vectorcamapp.core.data.dto.serializers

import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object FormQuestionScopeSerializer : KSerializer<FormQuestionScope> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("FormQuestionScope", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FormQuestionScope) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): FormQuestionScope {
        return FormQuestionScope.valueOf(decoder.decodeString())
    }
}