package com.vci.vectorcamapp.core.di

import com.vci.vectorcamapp.core.data.repository.SessionRepositoryImplementation
import com.vci.vectorcamapp.core.data.repository.SpecimenRepositoryImplementation
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImplementation: SessionRepositoryImplementation
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSpecimenRepository(
        specimenRepositoryImplementation: SpecimenRepositoryImplementation
    ): SpecimenRepository
}
