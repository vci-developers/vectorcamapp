package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
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