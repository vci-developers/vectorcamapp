package com.vci.vectorcamapp.imaging.data.util

/**
 * Rules for when a serialized GPU program or a stored accelerator verdict may be reused.
 *
 * Both are only valid for one install and one set of model bytes. An app update changes
 * [installToken]; replacing a `.tflite` asset changes [assetFingerprint]. Either mismatch means
 * the cache must be treated as a miss and the GPU/CPU sweep run again.
 */
internal object GpuCacheIdentity {

    private const val HEX_RADIX = 16

    fun installToken(versionCode: Int, lastUpdateTime: Long): String =
        "v$versionCode-${lastUpdateTime.toString(HEX_RADIX)}"

    /**
     * Cached accelerator verdicts (which GPU variant, or CPU) are only readable when we know they
     * were recorded for this install. A missing current token is treated as unusable: better to
     * sweep again than to run a new model on a variant chosen for an old one.
     */
    fun installTokenMatches(storedToken: String?, currentToken: String?): Boolean =
        currentToken != null && storedToken == currentToken

    /**
     * A stored variant for one asset is only reusable when the asset bytes still match. A null
     * current fingerprint (asset could not be measured) falls back to the install token alone.
     */
    fun assetVerdictValid(
        storedFingerprint: String?,
        currentFingerprint: String?,
    ): Boolean {
        if (currentFingerprint == null) return true
        return storedFingerprint == currentFingerprint
    }

    fun programCacheKey(cacheKey: String, assetFingerprint: String?): String =
        if (assetFingerprint == null) cacheKey else "$cacheKey@$assetFingerprint"
}
