package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vci.vectorcamapp.imaging.data.InferenceModelResolver
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
        inferenceModelResolver: InferenceModelResolver,
    ): SpecimenDetector {
        return TfLiteSpecimenDetector(context) {
            inferenceModelResolver.resolve({ it.detect }, "detect.tflite")
        }
    }

    @Provides
    @Singleton
    @SpeciesClassifier
    fun provideSpeciesClassifier(
        @ApplicationContext context: Context,
        inferenceModelResolver: InferenceModelResolver,
    ): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context = context,
            threadName = "LiteRTSpeciesClassifierThread",
            bundledClassCount = SpeciesLabel.entries.size,
            resolveModel = { inferenceModelResolver.resolve({ it.species }, "species.tflite") },
        )
    }

    @Provides
    @Singleton
    @SexClassifier
    fun provideSexClassifier(
        @ApplicationContext context: Context,
        inferenceModelResolver: InferenceModelResolver,
    ): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context = context,
            threadName = "LiteRTSexClassifierThread",
            bundledClassCount = SexLabel.entries.size,
            resolveModel = { inferenceModelResolver.resolve({ it.sex }, "sex.tflite") },
        )
    }

    @Provides
    @Singleton
    @AbdomenStatusClassifier
    fun provideAbdomenStatusClassifier(
        @ApplicationContext context: Context,
        inferenceModelResolver: InferenceModelResolver,
    ): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context = context,
            threadName = "LiteRTAbdomenStatusClassifierThread",
            bundledClassCount = AbdomenStatusLabel.entries.size,
            resolveModel = { inferenceModelResolver.resolve({ it.abdomenStatus }, "abdomen_status.tflite") },
        )
    }
}
