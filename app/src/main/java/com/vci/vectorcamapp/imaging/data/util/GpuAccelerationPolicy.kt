package com.vci.vectorcamapp.imaging.data.util

import android.app.ActivityManager
import android.content.Context
import timber.log.Timber

/**
 * Decides whether LiteRT's GPU accelerator should be attempted on this device.
 *
 * Some low-end SoCs create a LiteRT GPU [com.google.ai.edge.litert.CompiledModel] successfully
 * (i.e. `CompiledModel.create` never throws) but their GPU driver has very limited support for
 * the compute shaders LiteRT's GPU accelerator relies on, and/or can't run graphics and compute
 * concurrently. Shader compilation and the first inference pass can stall the shared
 * graphics/UI pipeline for hundreds of milliseconds to seconds, which is indistinguishable from
 * a freeze to the user. Because creation doesn't throw, the normal GPU-fails-so-fall-back-to-CPU
 * path in the detector/classifiers never triggers on its own.
 *
 * This policy covers live-camera detection only, where detection runs continuously alongside
 * CameraX preview updates and contention is what hurts. Post-capture classification has the
 * opposite problem - GPU there returns plausible but wrong logits - so it is decided by
 * correctness instead, in [ClassifierAcceleratorSelector].
 *
 * Rather than hardcoding a list of known-bad device models (which only protects against exactly
 * the devices we've already seen fail), gating uses two general signals:
 *
 * 1. A hardware-tier heuristic ([isLowTierDevice]) - budget SoCs that ship with GPUs too weak for
 *    LiteRT's GPU delegate are, in practice, always paired with a small amount of RAM. Gating on
 *    RAM tier (rather than a device/model allowlist) catches "similarly built" budget phones from
 *    any OEM without needing to know about them ahead of time.
 * 2. A one-time runtime benchmark ([recordGpuWarmup]) - the actual GPU warm-up time is measured
 *    the first time a model initializes, and if it's too slow (or throws), live-camera GPU
 *    acceleration is disabled and the decision is persisted. This self-heals for any device that
 *    slips past the RAM-tier heuristic but still has a slow/buggy GPU delegate for other reasons
 *    (driver bugs, thermal throttling, etc).
 */
object GpuAccelerationPolicy {

    private const val PREFS_NAME = "gpu_acceleration_policy"
    private const val KEY_FORCE_CPU = "force_cpu"
    private const val KEY_MEASURED_WARMUP_MS = "measured_warmup_ms"

    // A healthy GPU delegate warm-up (shader compilation + first inference) should stay well
    // under a second even on mid-range hardware. Anything at or above this is visible to the
    // user as jank/freezing, regardless of which device exhibits it.
    private const val SLOW_WARMUP_THRESHOLD_MS = 700L

    // Devices at or below this RAM tier are, in practice, always paired with GPUs (entry Adreno
    // 5xx/6xx, low-end Mali, etc.) too weak to run LiteRT's GPU delegate without stalling the
    // shared graphics pipeline - e.g. the Moto G Play (2021) at 3GB RAM. This is a proxy for
    // "budget device class", not a specific device.
    private const val LOW_TIER_TOTAL_RAM_MB = 4096L

    fun shouldAttemptGpu(context: Context): Boolean {
        val prefs = prefs(context)
        if (prefs.contains(KEY_FORCE_CPU)) {
            return !prefs.getBoolean(KEY_FORCE_CPU, false)
        }
        return !isLowTierDevice(context)
    }

    fun recordGpuWarmup(context: Context, elapsedMs: Long, succeeded: Boolean) {
        if (succeeded && elapsedMs < SLOW_WARMUP_THRESHOLD_MS) return

        Timber.w(
            "GPU warm-up ${if (succeeded) "slow (${elapsedMs}ms)" else "failed"}; " +
                "disabling live-camera GPU acceleration on this device going forward"
        )
        prefs(context).edit()
            .putBoolean(KEY_FORCE_CPU, true)
            .putLong(KEY_MEASURED_WARMUP_MS, elapsedMs)
            .apply()
    }

    private fun isLowTierDevice(context: Context): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false

        if (activityManager.isLowRamDevice) return true

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)

        return totalRamMb in 1..LOW_TIER_TOTAL_RAM_MB
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
