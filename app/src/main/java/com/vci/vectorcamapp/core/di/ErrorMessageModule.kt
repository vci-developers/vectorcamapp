package com.vci.vectorcamapp.core.di

import com.vci.vectorcamapp.core.data.DefaultErrorMessageEmitter
import com.vci.vectorcamapp.core.domain.util.ErrorMessageEmitter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorMessageModule {

    @Binds
    @Singleton
    abstract fun bindErrorMessageEmitter(impl: DefaultErrorMessageEmitter): ErrorMessageEmitter
}
