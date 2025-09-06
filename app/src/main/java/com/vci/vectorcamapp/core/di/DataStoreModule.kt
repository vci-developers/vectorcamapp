package com.vci.vectorcamapp.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.vci.vectorcamapp.core.data.cache.serializers.DeviceCacheDtoSerializer
import com.vci.vectorcamapp.core.data.cache.serializers.CurrentSessionCacheDtoSerializer
import com.vci.vectorcamapp.core.data.cache.serializers.IntakeDefaultCacheDtoSerializer
import com.vci.vectorcamapp.core.data.dto.cache.DeviceCacheDto
import com.vci.vectorcamapp.core.data.dto.cache.CurrentSessionCacheDto
import com.vci.vectorcamapp.core.data.dto.cache.IntakeDefaultCacheDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val CURRENT_SESSION_DATA_STORE_FILE_NAME = "current_session.pb"
private const val DEVICE_DATA_STORE_FILE_NAME = "device.pb"
private const val INTAKE_DEFAULT_DATA_STORE_FILE_NAME = "intake_form_default_values.pb"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideCurrentSessionDataStore(@ApplicationContext context: Context) : DataStore<CurrentSessionCacheDto> {
        return DataStoreFactory.create(
            serializer = CurrentSessionCacheDtoSerializer,
            produceFile = { context.dataStoreFile(CURRENT_SESSION_DATA_STORE_FILE_NAME)},
            corruptionHandler = ReplaceFileCorruptionHandler { CurrentSessionCacheDto() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideDeviceDataStore(@ApplicationContext context: Context) : DataStore<DeviceCacheDto> {
        return DataStoreFactory.create(
            serializer = DeviceCacheDtoSerializer,
            produceFile = { context.dataStoreFile(DEVICE_DATA_STORE_FILE_NAME)},
            corruptionHandler = ReplaceFileCorruptionHandler { DeviceCacheDto() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideIntakeDefaultDataStore(@ApplicationContext context: Context): DataStore<IntakeDefaultCacheDto> {
        return DataStoreFactory.create(
            serializer = IntakeDefaultCacheDtoSerializer,
            produceFile = { context.dataStoreFile(INTAKE_DEFAULT_DATA_STORE_FILE_NAME) },
            corruptionHandler = ReplaceFileCorruptionHandler { IntakeDefaultCacheDto() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
    }
}
