package com.vci.vectorcamapp.imaging.data

import android.os.Looper
import android.util.Log
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory

object GpuDelegateManager {

    private const val TAG = "GpuDelegateManager"
    
    @Volatile
    private var gpuDelegate: GpuDelegate? = null
    
    // Track the thread where GPU delegate was created to ensure consistent usage
    @Volatile
    private var gpuDelegateThread: Thread? = null

    fun getDelegate(): GpuDelegate {
        return gpuDelegate ?: synchronized(this) {
            gpuDelegate ?: createDelegate().also { 
                gpuDelegate = it
                gpuDelegateThread = Thread.currentThread()
                Log.d(TAG, "GPU delegate created on thread: ${gpuDelegateThread?.name}")
            }
        }
    }

    private fun createDelegate(): GpuDelegate {
        // Ensure we're not on the main thread for GPU delegate creation
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            Log.w(TAG, "Creating GPU delegate on main thread - this may cause performance issues")
        }
        
        val options = GpuDelegateFactory.Options().apply {
            // Use fast single answer preference for real-time detection
            inferencePreference = GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER
            // Keep precision as required - no precision loss allowed
            isPrecisionLossAllowed = false
            // Optimize for sustained performance rather than peak performance
            inferencePreference = GpuDelegateFactory.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED
        }
        
        return try {
            GpuDelegate(options).also {
                Log.d(TAG, "GPU delegate created successfully with sustained speed preference")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create GPU delegate: ${e.message}")
            throw e
        }
    }

    fun isCurrentThreadCompatible(): Boolean {
        return gpuDelegateThread == null || gpuDelegateThread == Thread.currentThread()
    }

    fun close() {
        synchronized(this) {
            try {
                gpuDelegate?.close()
                Log.d(TAG, "GPU delegate closed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GPU delegate: ${e.message}")
            } finally {
                gpuDelegate = null
                gpuDelegateThread = null
            }
        }
    }
}
