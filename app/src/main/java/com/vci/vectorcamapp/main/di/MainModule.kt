package com.vci.vectorcamapp.main.di

import com.vci.vectorcamapp.main.logging.DefaultMainSentryLogger
import com.vci.vectorcamapp.main.logging.MainSentryLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {

    @Binds
    @Singleton
    abstract fun bindMainSentryLogger(impl: DefaultMainSentryLogger): MainSentryLogger
}
