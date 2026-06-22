package com.vci.vectorcamapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.logging.crashlytics.Crashy
import com.vci.vectorcamapp.core.logging.crashlytics.CrashyContext
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class VectorCamApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Field injection is required for Application subclasses — Hilt does not support
    // constructor injection for Android framework entry-point classes.
    @Inject
    lateinit var deviceCache: DeviceCache

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        VectorCamAnalytics.appContext = applicationContext

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = true
        Crashy.crashlytics = crashlytics

        // Set region / build flavor context keys
        crashlytics.setCustomKey("region", BuildConfig.REGION)
        crashlytics.setCustomKey("region_code", BuildConfig.REGION_CODE)
        crashlytics.setCustomKey("build_flavor", BuildConfig.FLAVOR)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)

        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
        VectorCamAnalytics.analytics = firebaseAnalytics
        VectorCamAnalytics.setRegion(BuildConfig.REGION)
        VectorCamAnalytics.debugLogging = BuildConfig.DEBUG
        VectorCamAnalytics.setStaticProperties()
        VectorCamAnalytics.updateDeviceCondition()

        // Re-hydrate user identity on every cold start
        applicationScope.launch {
            deviceCache.getDevice()?.let { device ->
                val programId = deviceCache.getProgramId()
                VectorCamAnalytics.setDevice(device, programId)
                Crashy.setDevice(device)
            }
        }

        // App lifecycle events
        val lifecycleObserver = object : DefaultLifecycleObserver {
            private var foregroundedAt: Long = 0L

            override fun onStart(owner: LifecycleOwner) {
                foregroundedAt = System.currentTimeMillis()
                VectorCamAnalytics.logEvent("app_foregrounded")
            }

            override fun onStop(owner: LifecycleOwner) {
                val duration = if (foregroundedAt > 0L) System.currentTimeMillis() - foregroundedAt else 0L
                VectorCamAnalytics.logEvent(
                    "app_backgrounded",
                    mapOf("foreground_duration_ms" to duration)
                )
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        val initStartMs = System.currentTimeMillis()
        try {
            OpenCVLoader.initLocal()
        } catch (e: Exception) {
            Crashy.exception(
                throwable = e,
                context = CrashyContext(
                    screen = "AppStart", feature = "OpenCV Initialization", action = "initLocal()"
                ),
                tags = mapOf(
                    "module" to "OpenCV", "phase" to "startup"
                ),
                extras = mapOf(
                    "deviceModel" to android.os.Build.MODEL,
                    "sdkVersion" to android.os.Build.VERSION.SDK_INT,
                    "possible_causes" to "OpenCV not bundled properly in APK, initLocal() called too early, ABI mismatch, missing native libs"
                )
            )
            VectorCamAnalytics.logEvent(
                "opencv_init_failed",
                mapOf(
                    "error_class" to "OpenCVInitException",
                    "error_message" to (e.message?.take(100) ?: "unknown")
                )
            )
        }

        VectorCamAnalytics.logEvent(
            "app_launched",
            mapOf(
                "is_cold_start" to true,
                "init_duration_ms" to (System.currentTimeMillis() - initStartMs)
            )
        )
    }
}
