package com.vci.vectorcamapp.imaging.di

import com.vci.vectorcamapp.imaging.data.repository.CameraRepositoryImplementation
import com.vci.vectorcamapp.imaging.data.repository.InferenceRepositoryImplementation
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
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
}