package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
    fun provideSpecimenDetector(@ApplicationContext context: Context): SpecimenDetector {
        return TfLiteSpecimenDetector(context)
    }

    @Provides
    @Singleton
    @SpeciesClassifier
    fun provideSpeciesClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context,
            "species.tflite",
            "LiteRTSpeciesClassifierThread",
            expectedNumClasses = SpeciesLabel.entries.size,
        )
    }

    @Provides
    @Singleton
    @SexClassifier
    fun provideSexClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context,
            "sex.tflite",
            "LiteRTSexClassifierThread",
            expectedNumClasses = SexLabel.entries.size,
        )
    }

    @Provides
    @Singleton
    @AbdomenStatusClassifier
    fun provideAbdomenStatusClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context,
            "abdomen_status.tflite",
            "LiteRTAbdomenStatusClassifierThread",
            expectedNumClasses = AbdomenStatusLabel.entries.size,
        )
    }
}