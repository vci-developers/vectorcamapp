package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class InferenceModelResolverTest {

    @Test
    fun resolve_usesDownloadedClassOrderWhenTheModelFileExists() = runTest {
        val root = Files.createTempDirectory("resolver").toFile()
        val store = LocalProgramModelStore(File(root, "files"))
        val modelFile = store.modelFile(PROGRAM_ID, MODEL_ID)
        modelFile.parentFile?.mkdirs()
        modelFile.writeBytes(byteArrayOf(9, 8, 7))
        store.saveConfig(PROGRAM_ID, ProgramModelsConfig(species = MODEL_ID))
        store.saveMetadata(
            ProgramModelDto(
                programId = PROGRAM_ID,
                modelId = MODEL_ID,
                modelClasses = listOf("Culex", "Anopheles funestus"),
                fileMd5 = "abc",
            )
        )
        val deviceCache = FakeDeviceCache(PROGRAM_ID)

        val resolved = InferenceModelResolver(context(), deviceCache, store)
            .resolve({ it.species }, "species.tflite")

        assertThat(resolved.downloaded).isTrue()
        assertThat(resolved.path).isEqualTo(modelFile.absolutePath)
        assertThat(resolved.classLabels).containsExactly("Culex", "Anopheles funestus").inOrder()
    }

    @Test
    fun resolve_usesBundledAssetWhenNothingIsDownloaded() = runTest {
        val store = LocalProgramModelStore(File(Files.createTempDirectory("resolver-empty").toFile(), "files"))
        val deviceCache = FakeDeviceCache(PROGRAM_ID)

        val resolved = InferenceModelResolver(context(), deviceCache, store)
            .resolve({ it.species }, "species.tflite")

        assertThat(resolved.downloaded).isFalse()
        assertThat(resolved.path).isEqualTo("species.tflite")
        assertThat(resolved.classLabels).isEmpty()
    }

    private fun context(): Context {
        val assets = mockk<AssetManager>()
        every { assets.openFd(any()) } throws IOException("no asset in unit test")
        val context = mockk<Context>()
        every { context.assets } returns assets
        return context
    }

    private class FakeDeviceCache(private val programId: Int?) : DeviceCache {
        override suspend fun saveDevice(device: Device, programId: Int) = Unit

        override suspend fun getDevice(): Device? = null

        override suspend fun getProgramId(): Int? = programId

        override fun observeProgramId(): Flow<Int?> = flowOf(programId)
    }

    private companion object {
        const val PROGRAM_ID = 3
        const val MODEL_ID = "species-v2"
    }
}
