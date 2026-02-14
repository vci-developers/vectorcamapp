package com.vci.vectorcamapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.vci.vectorcamapp.main.logging.MainSentryLogger
import io.sentry.Sentry
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class VectorCamApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var mainSentryLogger: MainSentryLogger

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        Sentry.configureScope { scope ->
            scope.setTag("region", BuildConfig.REGION)
            scope.setTag("region_code", BuildConfig.REGION_CODE)
        }

        try {
            OpenCVLoader.initLocal()
        } catch (e: Exception) {
            mainSentryLogger.logOpenCvInitFailure(e)
        }

        try {
            val postHogConfig = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST
            )

            postHogConfig.captureApplicationLifecycleEvents = true

            postHogConfig.sessionReplay = false

            postHogConfig.sessionReplayConfig.maskAllImages = false
            postHogConfig.sessionReplayConfig.maskAllTextInputs = false
            postHogConfig.sessionReplayConfig.screenshot = true

            postHogConfig.optOut = BuildConfig.DEBUG

            PostHogAndroid.setup(this, postHogConfig)
        } catch (e: Exception) {
            mainSentryLogger.logPostHogInitFailure(e)
        }
    }
}
