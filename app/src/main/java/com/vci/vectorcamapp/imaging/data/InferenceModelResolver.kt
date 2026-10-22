package com.vci.vectorcamapp.imaging.data

import android.content.Context
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import com.vci.vectorcamapp.imaging.data.util.GpuModelCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The model file and class list to use for one inference role, resolved when the interpreter
 * is built rather than when the object graph is created.
 */
data class ResolvedInferenceModel(
    val path: String,
    val classLabels: List<String>,
    val downloaded: Boolean,
    /** Changes when the file path, bytes, or checksum change. Class order is not included. */
    val fileFingerprint: String,
)

@Singleton
class InferenceModelResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCache: DeviceCache,
    private val localProgramModelStore: LocalProgramModelStore,
) {
    fun resolve(
        roleModelId: (ProgramModelsConfig) -> String?,
        assetFallback: String,
    ): ResolvedInferenceModel {
        val programId = runBlocking { deviceCache.getProgramId() }
        return downloadedModel(programId, roleModelId) ?: bundled(assetFallback)
    }

    private fun downloadedModel(
        programId: Int?,
        roleModelId: (ProgramModelsConfig) -> String?,
    ): ResolvedInferenceModel? {
        if (programId == null || programId <= 0) return null
        val modelId = localProgramModelStore.getCachedConfigSync(programId)
            ?.let(roleModelId)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val localPath = localProgramModelStore.modelPathIfExists(programId, modelId) ?: return null
        val metadata = localProgramModelStore.getCachedMetadataSync(programId, modelId)
        return ResolvedInferenceModel(
            path = localPath,
            classLabels = metadata?.modelClasses.orEmpty(),
            downloaded = true,
            fileFingerprint = fingerprint(localPath, metadata?.fileMd5.orEmpty()),
        )
    }

    private fun bundled(assetFallback: String): ResolvedInferenceModel {
        return ResolvedInferenceModel(
            path = assetFallback,
            classLabels = emptyList(),
            downloaded = false,
            fileFingerprint = fingerprint(assetFallback, md5 = ""),
        )
    }

    private fun fingerprint(path: String, md5: String): String {
        val stat = GpuModelCache.assetFingerprint(context, path).orEmpty()
        return "$path|$stat|$md5"
    }
}
