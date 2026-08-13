package com.vci.vectorcamapp.core.data.cache.serializers

import androidx.datastore.core.Serializer
import com.vci.vectorcamapp.core.data.dto.cache.ProgramConfigCacheDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object ProgramConfigCacheDtoSerializer : Serializer<ProgramConfigCacheDto> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override val defaultValue: ProgramConfigCacheDto = ProgramConfigCacheDto()

    override suspend fun readFrom(input: InputStream): ProgramConfigCacheDto {
        return try {
            json.decodeFromString(
                deserializer = ProgramConfigCacheDto.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: ProgramConfigCacheDto, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(
                    serializer = ProgramConfigCacheDto.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }
}
