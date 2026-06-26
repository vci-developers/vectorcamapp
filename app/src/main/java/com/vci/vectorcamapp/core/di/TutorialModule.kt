package com.vci.vectorcamapp.core.di

import com.vci.vectorcamapp.core.data.tutorial.TutorialRepositoryImpl
import com.vci.vectorcamapp.core.domain.tutorial.TutorialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TutorialModule {

    @Binds
    @Singleton
    abstract fun bindTutorialRepository(impl: TutorialRepositoryImpl): TutorialRepository
}
