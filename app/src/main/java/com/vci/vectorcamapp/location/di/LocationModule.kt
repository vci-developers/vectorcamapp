package com.vci.vectorcamapp.location.di

import com.vci.vectorcamapp.location.data.repository.LocationRepositoryImplementation
import com.vci.vectorcamapp.location.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImplementation
    ): LocationRepository
}
