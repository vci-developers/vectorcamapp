package com.vci.vectorcamapp.imaging.domain

import com.vci.vectorcamapp.imaging.di.AbdomenStatusClassifier
import com.vci.vectorcamapp.imaging.di.Detector
import com.vci.vectorcamapp.imaging.di.SexClassifier
import com.vci.vectorcamapp.imaging.di.SpeciesClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Decides when the detector and classifiers are held in memory.
 *
 * Building them costs seconds. Doing it when the imaging screen opens puts that cost directly in
 * front of the first capture, so they are warmed at process start and overlap with splash,
 * permissions, and whatever screen the user lands on.
 *
 * They are deliberately not held for the life of the process. Together they hold a few hundred
 * megabytes, and a backgrounded app that size is a prime target for the low-memory killer - which
 * would cost a full rebuild and lose the session's camera state. So they are dropped once the app
 * has been in the background for [RELEASE_DELAY] and rebuilt when it returns. The delay is what
 * stops a glance at another app from throwing them away, and it is long enough that a capture in
 * flight when the user leaves still finishes against a live model.
 */
@Singleton
class SpecimenModelWarmer @Inject constructor(
    @Detector specimenDetector: SpecimenDetector,
    @SpeciesClassifier speciesClassifier: SpecimenClassifier,
    @SexClassifier sexClassifier: SpecimenClassifier,
    @AbdomenStatusClassifier abdomenStatusClassifier: SpecimenClassifier,
) {

    private val models: List<WarmableModel> = listOf(
        specimenDetector,
        speciesClassifier,
        sexClassifier,
        abdomenStatusClassifier,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val lock = Any()
    private var warmWanted = false
    private var pendingRelease: Job? = null

    /**
     * Marks the models as wanted and starts building any that are cold. Returns immediately and is
     * cheap to call repeatedly.
     */
    fun warm() {
        synchronized(lock) {
            cancelPendingRelease()
            warmWanted = true
        }
        models.forEach { it.warm() }
    }

    fun onForeground() {
        warm()
    }

    fun onBackground() {
        synchronized(lock) {
            if (!warmWanted) return
            cancelPendingRelease()
            pendingRelease = scope.launch {
                delay(RELEASE_DELAY)
                Timber.d("Releasing specimen models after $RELEASE_DELAY in the background")
                models.forEach { it.release() }
            }
        }
    }

    private fun cancelPendingRelease() {
        pendingRelease?.cancel()
        pendingRelease = null
    }

    private companion object {
        private val RELEASE_DELAY = 30.seconds
    }
}
