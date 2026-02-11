package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class LocationHierarchyConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val serializer = MapSerializer(String.serializer(), String.serializer())

    @TypeConverter
    fun fromLocationHierarchy(value: Map<String, String>?): String? {
        if (value == null) return null
        return runCatching { json.encodeToString(serializer, value) }.getOrNull()
    }

    @TypeConverter
    fun toLocationHierarchy(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(serializer, value) }
            .getOrElse { emptyMap() }
    }
}
