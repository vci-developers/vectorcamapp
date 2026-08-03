package com.vci.vectorcamapp.core.di

import com.vci.vectorcamapp.core.presentation.util.locale.AppLocaleManager
import com.vci.vectorcamapp.core.presentation.util.locale.AppLocaleManagerImplementation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocaleModule {

    @Binds
    @Singleton
    abstract fun bindAppLocaleManager(
        appLocaleManagerImplementation: AppLocaleManagerImplementation
    ): AppLocaleManager
}
