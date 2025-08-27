package com.vci.vectorcamapp.imaging.data

import android.util.Log
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory

object GpuDelegateManager {

    private const val TAG = "GpuDelegateManager"
    
    // Thread-local delegates to avoid OpenGL context conflicts
    private val threadLocalDelegates = ThreadLocal<GpuDelegate>()

    /**
     * Gets or creates a GPU delegate for the current thread.
     * This ensures each thread has its own GPU context to avoid OpenGL conflicts.
     */
    fun getDelegate(): GpuDelegate {
        return threadLocalDelegates.get() ?: synchronized(this) {
            threadLocalDelegates.get() ?: createDelegate().also { 
                threadLocalDelegates.set(it)
                Log.d(TAG, "Created new GPU delegate for thread: ${Thread.currentThread().name}")
            }
        }
    }

    private fun createDelegate(): GpuDelegate {
        val options = GpuDelegateFactory.Options().apply {
            // Enable fast inference for real-time processing
            inferencePreference = GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER
            // Allow precision loss for better performance - critical for real-time inference
            isPrecisionLossAllowed = true
            // Enable quantized models for better GPU performance
            setQuantizedModelsAllowed(true)
        }
        
        return try {
            GpuDelegate(options).also {
                Log.d(TAG, "GPU delegate created successfully with optimized settings")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create GPU delegate: ${e.message}")
            throw e
        }
    }

    /**
     * Closes the GPU delegate for the current thread
     */
    fun closeCurrentThreadDelegate() {
        threadLocalDelegates.get()?.let { delegate ->
            try {
                delegate.close()
                threadLocalDelegates.remove()
                Log.d(TAG, "Closed GPU delegate for thread: ${Thread.currentThread().name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GPU delegate: ${e.message}")
            }
        }
    }

    /**
     * Closes all GPU delegates - should only be called on app shutdown
     */
    fun closeAll() {
        synchronized(this) {
            try {
                closeCurrentThreadDelegate()
                Log.d(TAG, "All GPU delegates closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during delegate cleanup: ${e.message}")
            }
        }
    }
}
