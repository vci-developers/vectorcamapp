package com.vci.vectorcamapp.core.data.cache.serializers

import androidx.datastore.core.Serializer
import com.vci.vectorcamapp.core.data.dto.cache.IntakeDefaultCacheDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object IntakeDefaultCacheDtoSerializer : Serializer<IntakeDefaultCacheDto> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override val defaultValue: IntakeDefaultCacheDto = IntakeDefaultCacheDto()

    override suspend fun readFrom(input: InputStream): IntakeDefaultCacheDto {
        return try {
            json.decodeFromString(
                deserializer = IntakeDefaultCacheDto.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: IntakeDefaultCacheDto, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(
                    serializer = IntakeDefaultCacheDto.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }
}
