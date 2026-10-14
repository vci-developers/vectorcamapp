package com.vci.vectorcamapp.core.data.program_model

import android.content.Context
import com.vci.vectorcamapp.core.data.dto.program.ProgramModelsConfigDto
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProgramModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun modelFile(programId: Int, modelId: String): File {
        return File(modelDir(programId, modelId), MODEL_FILE_NAME)
    }

    fun tempModelFile(programId: Int, modelId: String): File {
        return File(modelDir(programId, modelId), TEMP_MODEL_FILE_NAME)
    }

    fun getCachedConfigSync(programId: Int): ProgramModelsConfig? {
        val file = configFile(programId)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(ProgramModelsConfigDto.serializer(), file.readText()).toDomain()
        }.getOrNull()
    }

    fun modelPathIfExists(programId: Int, modelId: String): String? {
        val file = modelFile(programId, modelId)
        return if (file.exists()) file.absolutePath else null
    }

    suspend fun getCachedConfig(programId: Int): ProgramModelsConfig? = withContext(Dispatchers.IO) {
        getCachedConfigSync(programId)
    }

    suspend fun saveConfig(programId: Int, config: ProgramModelsConfig): Unit =
        withContext(Dispatchers.IO) {
            val file = configFile(programId)
            file.parentFile?.mkdirs()
            val dto = ProgramModelsConfigDto(
                species = config.species,
                sex = config.sex,
                abdomenStatus = config.abdomenStatus,
                detect = config.detect,
            )
            file.writeText(json.encodeToString(ProgramModelsConfigDto.serializer(), dto))
        }

    suspend fun getCachedMetadata(programId: Int, modelId: String): ProgramModelDto? =
        withContext(Dispatchers.IO) {
            val metadataFile = metadataFile(programId, modelId)
            if (!metadataFile.exists() || !modelFile(programId, modelId).exists()) {
                return@withContext null
            }
            runCatching {
                json.decodeFromString(ProgramModelDto.serializer(), metadataFile.readText())
            }.getOrNull()
        }

    suspend fun saveMetadata(metadata: ProgramModelDto): Unit = withContext(Dispatchers.IO) {
        val metadataFile = metadataFile(metadata.programId, metadata.modelId)
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(json.encodeToString(ProgramModelDto.serializer(), metadata))
    }

    suspend fun hasMatchingModel(programId: Int, modelId: String, expectedMd5: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = modelFile(programId, modelId)
            if (!file.exists()) return@withContext false
            computeMd5(file).equals(expectedMd5, ignoreCase = true)
        }

    suspend fun promoteTempFile(programId: Int, modelId: String, expectedMd5: String): Boolean =
        withContext(Dispatchers.IO) {
            val tempFile = tempModelFile(programId, modelId)
            if (!tempFile.exists()) return@withContext false

            val actualMd5 = computeMd5(tempFile)
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                tempFile.delete()
                return@withContext false
            }

            val target = modelFile(programId, modelId)
            if (target.exists()) {
                target.delete()
            }
            tempFile.renameTo(target)
        }

    suspend fun clear(programId: Int): Unit = withContext(Dispatchers.IO) {
        programDir(programId).deleteRecursively()
    }

    private fun programDir(programId: Int): File {
        return File(context.filesDir, "$MODELS_DIR/$programId")
    }

    private fun modelDir(programId: Int, modelId: String): File {
        return File(programDir(programId), modelId)
    }

    private fun configFile(programId: Int): File {
        return File(programDir(programId), CONFIG_FILE_NAME)
    }

    private fun metadataFile(programId: Int, modelId: String): File {
        return File(modelDir(programId, modelId), METADATA_FILE_NAME)
    }

    private fun computeMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val MODELS_DIR = "program_models"
        const val MODEL_FILE_NAME = "model.tflite"
        const val TEMP_MODEL_FILE_NAME = "model.tflite.tmp"
        const val METADATA_FILE_NAME = "metadata.json"
        const val CONFIG_FILE_NAME = "config.json"
        const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}
