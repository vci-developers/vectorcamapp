package com.vci.vectorcamapp.imaging.di

import android.content.Context
import com.vci.vectorcamapp.imaging.data.camera.Camera2Controller
import com.vci.vectorcamapp.imaging.data.repository.CameraRepositoryImplementation
import com.vci.vectorcamapp.imaging.data.repository.InferenceRepositoryImplementation
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ImagingRepositoryModule {

    @Binds
    @ViewModelScoped
    abstract fun bindCameraRepository(
        cameraRepositoryImplementation: CameraRepositoryImplementation
    ) : CameraRepository

    @Binds
    @ViewModelScoped
    abstract fun bindInferenceRepository(
        inferenceRepositoryImplementation: InferenceRepositoryImplementation
    ) : InferenceRepository

    companion object {
        @Provides
        @ViewModelScoped
        fun provideCamera2Controller(@ApplicationContext context: Context): Camera2Controller {
            return Camera2Controller(context)
        }
    }
}
