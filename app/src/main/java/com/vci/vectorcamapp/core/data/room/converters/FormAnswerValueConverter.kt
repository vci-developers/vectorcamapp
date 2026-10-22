package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import com.vci.vectorcamapp.core.data.dto.serializers.FormAnswerValueSerializer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import kotlinx.serialization.json.Json

class FormAnswerValueConverter {
    @TypeConverter
    fun fromFormAnswerValue(value: FormAnswerValue): String =
        Json.encodeToString(FormAnswerValueSerializer, value)

    @TypeConverter
    fun toFormAnswerValue(value: String): FormAnswerValue =
        Json.decodeFromString(FormAnswerValueSerializer, value)
}
