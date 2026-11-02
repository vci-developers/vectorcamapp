package com.vci.vectorcamapp.core.di

import com.vci.vectorcamapp.core.presentation.util.error.DefaultErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.success.DefaultSuccessMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.success.SuccessMessageEmitter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {

    @Binds
    @Singleton
    abstract fun bindErrorMessageEmitter(impl: DefaultErrorMessageEmitter): ErrorMessageEmitter

    @Binds
    @Singleton
    abstract fun bindSuccessMessageEmitter(impl: DefaultSuccessMessageEmitter): SuccessMessageEmitter
}
