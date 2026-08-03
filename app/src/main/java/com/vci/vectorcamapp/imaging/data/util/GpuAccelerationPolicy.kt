package com.vci.vectorcamapp.imaging.data.util

import android.os.Build

/**
 * Some low-end SoCs create a LiteRT GPU [com.google.ai.edge.litert.CompiledModel] successfully
 * (i.e. `CompiledModel.create` never throws) but their GPU driver has very limited support for
 * the compute shaders LiteRT's GPU accelerator relies on. Shader compilation and buffer
 * conversion on first inference can stall the device for seconds at a time, which is
 * indistinguishable from a freeze to the user. Because creation doesn't throw, the normal
 * GPU-fails-so-fall-back-to-CPU path in the detector/classifiers never triggers, so we instead
 * deny-list known-problematic devices up front and force CPU before attempting GPU creation.
 */
object GpuAccelerationPolicy {

    // Build.DEVICE codenames of hardware known to freeze on LiteRT's GPU accelerator, matched by
    // lowercase prefix to tolerate carrier/region suffixes (e.g. "guamnavzw", "guamnaretail").
    private val DEVICE_CODENAME_PREFIX_DENYLIST = setOf(
        // Moto G Play (2021) / XT2093 - Qualcomm Snapdragon 460 (SM4250) with an Adreno 610 GPU.
        "guamna",
    )

    // Build.MODEL substrings as a fallback in case the codename ever diverges from the above.
    private val MODEL_NAME_SUBSTRING_DENYLIST = setOf(
        "moto g play (2021)",
        "moto g play 2021",
    )

    fun shouldForceCpu(): Boolean {
        val device = Build.DEVICE?.lowercase().orEmpty()
        val model = Build.MODEL?.lowercase().orEmpty()

        return DEVICE_CODENAME_PREFIX_DENYLIST.any { device.startsWith(it) } ||
            MODEL_NAME_SUBSTRING_DENYLIST.any { model.contains(it) }
    }
}
