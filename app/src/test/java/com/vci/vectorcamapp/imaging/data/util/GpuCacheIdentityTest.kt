package com.vci.vectorcamapp.imaging.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuCacheIdentityTest {

    @Test
    fun installToken_includesVersionCodeAndUpdateTime() {
        assertEquals("v42-ff", GpuCacheIdentity.installToken(42, 255L))
    }

    @Test
    fun installToken_changesWhenVersionCodeChanges() {
        val previous = GpuCacheIdentity.installToken(10, 1_000L)
        val updated = GpuCacheIdentity.installToken(11, 1_000L)
        assertFalse(GpuCacheIdentity.installTokenMatches(previous, updated))
    }

    @Test
    fun installToken_changesWhenLastUpdateTimeChanges() {
        val previous = GpuCacheIdentity.installToken(10, 1_000L)
        val reinstalled = GpuCacheIdentity.installToken(10, 2_000L)
        assertFalse(GpuCacheIdentity.installTokenMatches(previous, reinstalled))
    }

    @Test
    fun installToken_matchesTheSameInstall() {
        val token = GpuCacheIdentity.installToken(10, 1_000L)
        assertTrue(GpuCacheIdentity.installTokenMatches(token, token))
    }

    @Test
    fun installToken_missingCurrentTokenIsUnusable() {
        assertFalse(GpuCacheIdentity.installTokenMatches("v10-3e8", null))
        assertFalse(GpuCacheIdentity.installTokenMatches(null, null))
    }

    @Test
    fun assetVerdict_mismatchInvalidatesGpuChoice() {
        assertFalse(GpuCacheIdentity.assetVerdictValid("26186536", "60013296"))
    }

    @Test
    fun assetVerdict_sameFingerprintIsReusable() {
        assertTrue(GpuCacheIdentity.assetVerdictValid("60013296", "60013296"))
    }

    @Test
    fun assetVerdict_missingStoredFingerprintAfterCodeUpdateForcesResweep() {
        assertFalse(GpuCacheIdentity.assetVerdictValid(null, "60013296"))
    }

    @Test
    fun assetVerdict_unreadableAssetFallsBackToInstallToken() {
        assertTrue(GpuCacheIdentity.assetVerdictValid("60013296", null))
        assertTrue(GpuCacheIdentity.assetVerdictValid(null, null))
    }

    @Test
    fun programCacheKey_includesFingerprintWhenPresent() {
        assertEquals(
            "species.tflite#gpu-fp32@60013296",
            GpuCacheIdentity.programCacheKey("species.tflite#gpu-fp32", "60013296"),
        )
        assertEquals(
            "detect.tflite",
            GpuCacheIdentity.programCacheKey("detect.tflite", null),
        )
    }
}
