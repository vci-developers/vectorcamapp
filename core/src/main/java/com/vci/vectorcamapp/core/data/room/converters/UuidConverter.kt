package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import java.util.UUID

class UuidConverter {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }
}
