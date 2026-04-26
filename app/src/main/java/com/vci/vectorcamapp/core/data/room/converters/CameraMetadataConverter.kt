package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import com.vci.vectorcamapp.core.data.dto.specimen_image.ImageMetadataDto
import com.vci.vectorcamapp.core.data.mappers.toCameraMetadata
import com.vci.vectorcamapp.core.data.mappers.toImageMetadataDto
import com.vci.vectorcamapp.imaging.domain.model.CameraMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CameraMetadataConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    @TypeConverter
    fun fromCameraMetadata(value: CameraMetadata?): String? {
        if (value == null) return null
        return runCatching { json.encodeToString(value.toImageMetadataDto()) }
            .getOrNull()
    }

    @TypeConverter
    fun toCameraMetadata(value: String?): CameraMetadata? {
        if (value.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<ImageMetadataDto>(value).toCameraMetadata() }
            .getOrNull()
    }
}
