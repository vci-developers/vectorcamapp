package com.vci.vectorcamapp.core.data.program_model

import android.content.Context
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
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

    fun modelFile(programId: Int): File {
        return File(programDir(programId), MODEL_FILE_NAME)
    }

    fun tempModelFile(programId: Int): File {
        return File(programDir(programId), TEMP_MODEL_FILE_NAME)
    }

    suspend fun getCachedMetadata(programId: Int): ProgramModelDto? = withContext(Dispatchers.IO) {
        val metadataFile = metadataFile(programId)
        if (!metadataFile.exists() || !modelFile(programId).exists()) return@withContext null
        runCatching {
            json.decodeFromString(ProgramModelDto.serializer(), metadataFile.readText())
        }.getOrNull()
    }

    suspend fun saveMetadata(metadata: ProgramModelDto): Unit = withContext(Dispatchers.IO) {
        val metadataFile = metadataFile(metadata.programId)
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(json.encodeToString(ProgramModelDto.serializer(), metadata))
    }

    suspend fun hasMatchingModel(programId: Int, expectedMd5: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = modelFile(programId)
            if (!file.exists()) return@withContext false
            computeMd5(file).equals(expectedMd5, ignoreCase = true)
        }

    suspend fun promoteTempFile(programId: Int, expectedMd5: String): Boolean =
        withContext(Dispatchers.IO) {
            val tempFile = tempModelFile(programId)
            if (!tempFile.exists()) return@withContext false

            val actualMd5 = computeMd5(tempFile)
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                tempFile.delete()
                return@withContext false
            }

            val target = modelFile(programId)
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

    private fun metadataFile(programId: Int): File {
        return File(programDir(programId), METADATA_FILE_NAME)
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
        const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}
