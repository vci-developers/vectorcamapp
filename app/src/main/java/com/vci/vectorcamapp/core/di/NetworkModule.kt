package com.vci.vectorcamapp.core.di

import android.content.Context
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.network.connectivity.AndroidConnectivityObserver
import com.vci.vectorcamapp.core.data.upload.image.util.TimeoutConfiguredTusClient
import com.vci.vectorcamapp.core.domain.network.connectivity.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.tus.android.client.TusPreferencesURLStore
import io.tus.java.client.TusClient
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MODEL_DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000L

    @Provides
    @Named("ConnectTimeout")
    fun provideConnectTimeout(): Int = CONNECT_TIMEOUT_MS

    @Provides
    @Named("ReadTimeout")
    fun provideReadTimeout(): Int = READ_TIMEOUT_MS

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            engine {
                connectTimeout = CONNECT_TIMEOUT_MS
                socketTimeout = READ_TIMEOUT_MS
            }
            install(HttpTimeout) {
                requestTimeoutMillis = READ_TIMEOUT_MS.toLong()
                connectTimeoutMillis = CONNECT_TIMEOUT_MS.toLong()
                socketTimeoutMillis = READ_TIMEOUT_MS.toLong()
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.ANDROID
            }
            install(ContentNegotiation) {
                json(json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                    explicitNulls = false
                })
            }
            install(DefaultRequest) {
                bearerAuth(BuildConfig.VECTORCAM_API_KEY)
                contentType(ContentType.Application.Json)
            }
        }
    }

    /**
     * Separate client for program-model downloads:
     * - does not auto-follow redirects (need 302 Location for the presigned S3 URL)
     * - no default JSON Content-Type / Bearer auth (S3 uses the presigned query string)
     */
    @Provides
    @Singleton
    @Named("ModelDownloadHttpClient")
    fun provideModelDownloadHttpClient(): HttpClient {
        return HttpClient(Android) {
            followRedirects = false
            expectSuccess = false
            engine {
                connectTimeout = CONNECT_TIMEOUT_MS
                socketTimeout = MODEL_DOWNLOAD_TIMEOUT_MS.toInt().coerceAtMost(Int.MAX_VALUE)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = MODEL_DOWNLOAD_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS.toLong()
                socketTimeoutMillis = MODEL_DOWNLOAD_TIMEOUT_MS
            }
            install(Logging) {
                level = LogLevel.HEADERS
                logger = Logger.ANDROID
            }
        }
    }

    @Provides
    fun provideTusClient(
        @ApplicationContext ctx: Context,
        client: TimeoutConfiguredTusClient
    ): TusClient = client.apply {
        headers = mapOf(
            "Content-Type" to "application/offset+octet-stream",
            "Authorization" to "Bearer ${BuildConfig.VECTORCAM_API_KEY}"
        )
        enableResuming(
            TusPreferencesURLStore(
                ctx.getSharedPreferences("tus_worker", Context.MODE_PRIVATE)
            )
        )
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver {
        return AndroidConnectivityObserver(context)
    }
}
