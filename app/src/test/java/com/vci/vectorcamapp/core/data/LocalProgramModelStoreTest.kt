package com.vci.vectorcamapp.core.data

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class LocalProgramModelStoreTest {

    @Test
    fun promoteTempFile_replacesWorkingModelAndRemovesTemp() = runTest {
        val store = store()
        val previous = "previous-model".toByteArray()
        val downloaded = "downloaded-model".toByteArray()
        store.modelFile(PROGRAM_ID, MODEL_ID).apply {
            parentFile?.mkdirs()
            writeBytes(previous)
        }
        store.tempModelFile(PROGRAM_ID, MODEL_ID).writeBytes(downloaded)

        val promoted = store.promoteTempFile(PROGRAM_ID, MODEL_ID, md5(downloaded))

        assertThat(promoted).isTrue()
        assertThat(store.modelFile(PROGRAM_ID, MODEL_ID).readBytes()).isEqualTo(downloaded)
        assertThat(store.tempModelFile(PROGRAM_ID, MODEL_ID).exists()).isFalse()
        assertThat(File(store.modelFile(PROGRAM_ID, MODEL_ID).parentFile, "model.tflite.bak").exists()).isFalse()
    }

    @Test
    fun promoteTempFile_keepsWorkingModelWhenChecksumDoesNotMatch() = runTest {
        val store = store()
        val previous = "previous-model".toByteArray()
        val downloaded = "corrupt-download".toByteArray()
        val target = store.modelFile(PROGRAM_ID, MODEL_ID)
        target.parentFile?.mkdirs()
        target.writeBytes(previous)
        store.tempModelFile(PROGRAM_ID, MODEL_ID).writeBytes(downloaded)

        val promoted = store.promoteTempFile(PROGRAM_ID, MODEL_ID, md5(previous))

        assertThat(promoted).isFalse()
        assertThat(target.readBytes()).isEqualTo(previous)
        assertThat(store.tempModelFile(PROGRAM_ID, MODEL_ID).exists()).isFalse()
    }

    @Test
    fun saveConfigAndMetadata_roundTripAndLeaveNoTempFiles() = runTest {
        val store = store()
        val config = ProgramModelsConfig(species = "species-v2", sex = "sex-v2")
        store.saveConfig(PROGRAM_ID, config)
        val modelFile = store.modelFile(PROGRAM_ID, MODEL_ID)
        modelFile.parentFile?.mkdirs()
        modelFile.writeBytes(byteArrayOf(1, 2, 3))
        store.saveMetadata(
            ProgramModelDto(
                programId = PROGRAM_ID,
                modelId = MODEL_ID,
                modelClasses = listOf("Culex", "Anopheles funestus"),
                fileMd5 = "abc",
            )
        )

        assertThat(store.getCachedConfigSync(PROGRAM_ID)).isEqualTo(config)
        assertThat(store.getCachedMetadataSync(PROGRAM_ID, MODEL_ID)?.modelClasses)
            .containsExactly("Culex", "Anopheles funestus")
            .inOrder()
        val programDir = store.modelFile(PROGRAM_ID, MODEL_ID).parentFile?.parentFile
        val leftovers = programDir
            ?.walk()
            ?.filter { it.name.endsWith(".tmp") || it.name.endsWith(".bak") }
            ?.toList()
        assertThat(leftovers).isEmpty()
    }

    private fun store(): LocalProgramModelStore {
        val filesDir = File(Files.createTempDirectory("program-models").toFile(), "files")
        return LocalProgramModelStore(filesDir)
    }

    private fun md5(bytes: ByteArray): String {
        return MessageDigest.getInstance("MD5").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PROGRAM_ID = 7
        const val MODEL_ID = "species-v2"
    }
}
