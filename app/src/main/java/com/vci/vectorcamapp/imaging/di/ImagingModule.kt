package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenClassifier
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenDetector
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ImagingModule {

    @Provides
    @ViewModelScoped
    @SpecimenIdRecognizer
    fun provideSpecimenIdRecognizer(): TextRecognizer {
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @Provides
    @ViewModelScoped
    @Detector
    fun provideSpecimenDetector(@ApplicationContext context: Context): SpecimenDetector {
        return TfLiteSpecimenDetector(context)
    }

    @Provides
    @ViewModelScoped
    @SpeciesClassifier
    fun provideSpeciesClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(context, "species.tflite", "TFLiteSpeciesClassifierThread")
    }

    @Provides
    @ViewModelScoped
    @SexClassifier
    fun provideSexClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(context, "sex.tflite", "TFLiteSexClassifierThread")
    }

    @Provides
    @ViewModelScoped
    @AbdomenStatusClassifier
    fun provideAbdomenStatusClassifier(@ApplicationContext context: Context): SpecimenClassifier {
        return TfLiteSpecimenClassifier(
            context, "abdomen_status.tflite", "TFLiteAbdomenStatusClassifierThread"
        )
    }
}