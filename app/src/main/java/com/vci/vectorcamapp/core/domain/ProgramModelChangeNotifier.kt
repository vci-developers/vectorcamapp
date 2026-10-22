package com.vci.vectorcamapp.core.domain

import com.vci.vectorcamapp.core.logging.ProgramModelLog
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells in-memory inference to reload after a program-model sync changes what is on disk.
 * Listeners run on the sync caller and must return quickly; model rebuilds are posted elsewhere.
 */
@Singleton
class ProgramModelChangeNotifier @Inject constructor() {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun notifyChanged() {
        listeners.forEach { listener ->
            try {
                listener()
            } catch (ignored: Exception) {
                ProgramModelLog.e(ignored, "Failed to apply a program model update")
            }
        }
    }
}
