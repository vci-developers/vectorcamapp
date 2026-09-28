package com.vci.vectorcamapp.imaging.data.util

import android.app.ActivityManager
import android.content.Context

/**
 * Decides whether LiteRT's GPU accelerator should be attempted for live-camera detection.
 *
 * Some low-end SoCs create a LiteRT GPU [com.google.ai.edge.litert.CompiledModel] successfully
 * (i.e. `CompiledModel.create` never throws) but their GPU driver has very limited support for
 * the compute shaders LiteRT's GPU accelerator relies on, and/or can't run graphics and compute
 * concurrently. Running detection on every preview frame then stalls the shared graphics/UI
 * pipeline badly enough to look like a freeze. Because creation doesn't throw, the normal
 * GPU-fails-so-fall-back-to-CPU path in the detector never triggers on its own.
 *
 * Budget SoCs with GPUs too weak for the delegate are, in practice, always paired with a small
 * amount of RAM, so [isLowTierDevice] gates on RAM tier rather than a device/model allowlist -
 * that catches similarly built budget phones from any OEM without knowing about them in advance.
 *
 * Deliberately not gated on measured warm-up time: that measurement is dominated by one-time
 * shader compilation, which costs over a second even on fast hardware (~1.2s on a Pixel 7a) and
 * says nothing about per-frame cost, so it disabled GPU on devices that handle it fine.
 *
 * This covers live-camera detection only. Post-capture classification has a different problem -
 * GPU precision there can produce NaN logits - so it is decided by correctness against a CPU
 * reference in [ClassifierAcceleratorSelector].
 */
object GpuAccelerationPolicy {

    // Devices at or below this RAM tier are, in practice, always paired with GPUs (entry Adreno
    // 5xx/6xx, low-end Mali, etc.) too weak to run LiteRT's GPU delegate without stalling the
    // shared graphics pipeline - e.g. the Moto G Play (2021) at 3GB RAM. This is a proxy for
    // "budget device class", not a specific device.
    private const val LOW_TIER_TOTAL_RAM_MB = 4096L

    private const val BYTES_PER_MB = 1024 * 1024

    fun shouldAttemptGpu(context: Context): Boolean = !isLowTierDevice(context)

    private fun isLowTierDevice(context: Context): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false

        if (activityManager.isLowRamDevice) return true

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / BYTES_PER_MB

        return totalRamMb in 1..LOW_TIER_TOTAL_RAM_MB
    }
}
