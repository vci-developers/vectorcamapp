package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import com.vci.vectorcamapp.core.logging.ProgramModelLog
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenClassifier
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenDetector
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import com.vci.vectorcamapp.imaging.domain.enums.AbdomenStatusLabel
import com.vci.vectorcamapp.imaging.domain.enums.SexLabel
import com.vci.vectorcamapp.imaging.domain.enums.SpeciesLabel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * These live for the whole process rather than per imaging screen, so that a rebuild is not forced
 * every time the screen is opened. They start cold and hold nothing until warmed;
 * [com.vci.vectorcamapp.imaging.domain.SpecimenModelWarmer] owns when they build and release.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImagingModule {

    @Provides
    @Singleton
    @SpecimenIdRecognizer
    fun provideSpecimenIdRecognizer(): TextRecognizer {
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @Provides
    @Singleton
    @Detector
    fun provideSpecimenDetector(
        @ApplicationContext context: Context,
        deviceCache: DeviceCache,
        localProgramModelStore: LocalProgramModelStore,
    ): SpecimenDetector {
        val path = resolveModelPath(
            deviceCache = deviceCache,
            localProgramModelStore = localProgramModelStore,
            roleModelId = { it.detect },
            assetFallback = "detect.tflite",
        )
        return TfLiteSpecimenDetector(context, path)
    }

    @Provides
    @Singleton
    @SpeciesClassifier
    fun provideSpeciesClassifier(
        @ApplicationContext context: Context,
        deviceCache: DeviceCache,
        localProgramModelStore: LocalProgramModelStore,
    ): SpecimenClassifier {
        val path = resolveModelPath(
            deviceCache = deviceCache,
            localProgramModelStore = localProgramModelStore,
            roleModelId = { it.species },
            assetFallback = "species.tflite",
        )
        return TfLiteSpecimenClassifier(
            context,
            path,
            "LiteRTSpeciesClassifierThread",
            expectedNumClasses = SpeciesLabel.entries.size,
        )
    }

    @Provides
    @Singleton
    @SexClassifier
    fun provideSexClassifier(
        @ApplicationContext context: Context,
        deviceCache: DeviceCache,
        localProgramModelStore: LocalProgramModelStore,
    ): SpecimenClassifier {
        val path = resolveModelPath(
            deviceCache = deviceCache,
            localProgramModelStore = localProgramModelStore,
            roleModelId = { it.sex },
            assetFallback = "sex.tflite",
        )
        return TfLiteSpecimenClassifier(
            context,
            path,
            "LiteRTSexClassifierThread",
            expectedNumClasses = SexLabel.entries.size,
        )
    }

    @Provides
    @Singleton
    @AbdomenStatusClassifier
    fun provideAbdomenStatusClassifier(
        @ApplicationContext context: Context,
        deviceCache: DeviceCache,
        localProgramModelStore: LocalProgramModelStore,
    ): SpecimenClassifier {
        val path = resolveModelPath(
            deviceCache = deviceCache,
            localProgramModelStore = localProgramModelStore,
            roleModelId = { it.abdomenStatus },
            assetFallback = "abdomen_status.tflite",
        )
        return TfLiteSpecimenClassifier(
            context,
            path,
            "LiteRTAbdomenStatusClassifierThread",
            expectedNumClasses = AbdomenStatusLabel.entries.size,
        )
    }

    private fun resolveModelPath(
        deviceCache: DeviceCache,
        localProgramModelStore: LocalProgramModelStore,
        roleModelId: (ProgramModelsConfig) -> String?,
        assetFallback: String,
    ): String {
        val programId = runBlocking { deviceCache.getProgramId() }
        if (programId == null || programId <= 0) {
            ProgramModelLog.i(
                "Imaging using bundled asset=%s (no programId registered)",
                assetFallback
            )
            return assetFallback
        }

        val config = localProgramModelStore.getCachedConfigSync(programId)
        if (config == null) {
            ProgramModelLog.i(
                "Imaging using bundled asset=%s (no local config.json for programId=%d)",
                assetFallback,
                programId
            )
            return assetFallback
        }

        val modelId = roleModelId(config)?.trim()?.takeIf { it.isNotEmpty() }
        if (modelId == null) {
            ProgramModelLog.i(
                "Imaging using bundled asset=%s (role not configured in config.json for programId=%d)",
                assetFallback,
                programId
            )
            return assetFallback
        }

        val localPath = localProgramModelStore.modelPathIfExists(programId, modelId)
        return if (localPath != null) {
            ProgramModelLog.i(
                "Imaging using downloaded model roleFallback=%s modelId=%s path=%s",
                assetFallback,
                modelId,
                localPath
            )
            localPath
        } else {
            ProgramModelLog.i(
                "Imaging using bundled asset=%s (no local file for modelId=%s programId=%d)",
                assetFallback,
                modelId,
                programId
            )
            assetFallback
        }
    }
}
