package com.vci.vectorcamapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.CrashyContext
import io.sentry.Sentry
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class VectorCamApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Crashy.crashlytics = FirebaseCrashlytics.getInstance()

        Sentry.configureScope { scope ->
            scope.setTag("region", BuildConfig.REGION)
            scope.setTag("region_code", BuildConfig.REGION_CODE)
        }

        try {
            OpenCVLoader.initLocal()
        } catch (e: Exception) {
            Crashy.exception(
                throwable = e, context = CrashyContext(
                    screen = "AppStart", feature = "OpenCV Initialization", action = "initLocal()"
                ), tags = mapOf(
                    "module" to "OpenCV", "phase" to "startup"
                ), extras = mapOf(
                    "deviceModel" to android.os.Build.MODEL,
                    "sdkVersion" to android.os.Build.VERSION.SDK_INT,
                    "possible_causes" to "OpenCV not bundled properly in APK, initLocal() called too early, ABI mismatch, missing native libs"
                )
            )
        }
    }
}
