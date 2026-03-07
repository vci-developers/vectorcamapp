package com.vci.vectorcamapp.core.data.cache.serializers

import androidx.datastore.core.Serializer
import com.vci.vectorcamapp.core.data.dto.cache.DefaultIntakeFieldsCacheDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object DefaultIntakeFieldsCacheDtoSerializer : Serializer<DefaultIntakeFieldsCacheDto> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override val defaultValue: DefaultIntakeFieldsCacheDto = DefaultIntakeFieldsCacheDto()

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun readFrom(input: InputStream): DefaultIntakeFieldsCacheDto {
        return try {
            json.decodeFromString(
                deserializer = DefaultIntakeFieldsCacheDto.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: DefaultIntakeFieldsCacheDto, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(
                    serializer = DefaultIntakeFieldsCacheDto.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }
}
