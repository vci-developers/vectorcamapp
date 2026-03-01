package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class StringListConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return runCatching { json.encodeToString(serializer, value) }.getOrNull()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(serializer, value) }
            .getOrElse { emptyList() }
    }
}
