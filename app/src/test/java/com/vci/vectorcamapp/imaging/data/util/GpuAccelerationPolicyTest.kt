package com.vci.vectorcamapp.imaging.data.util

import android.app.ActivityManager
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class GpuAccelerationPolicyTest {

    private val context = mockk<Context>()
    private val activityManager = mockk<ActivityManager>()

    @Test
    fun missingActivityManager_attemptsGpu() {
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isTrue()
    }

    @Test
    fun lowRamDevice_skipsGpu() {
        stubActivityManager(lowRamDevice = true, totalRamMb = 8192)

        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isFalse()
    }

    @Test
    fun fourGigabytesOrLess_skipsGpu() {
        stubActivityManager(lowRamDevice = false, totalRamMb = 4096)
        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isFalse()

        stubActivityManager(lowRamDevice = false, totalRamMb = 3072)
        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isFalse()
    }

    @Test
    fun aboveFourGigabytes_attemptsGpu() {
        stubActivityManager(lowRamDevice = false, totalRamMb = 4097)

        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isTrue()
    }

    @Test
    fun unreadableRam_attemptsGpu() {
        stubActivityManager(lowRamDevice = false, totalRamMb = 0)

        assertThat(GpuAccelerationPolicy.shouldAttemptGpu(context)).isTrue()
    }

    private fun stubActivityManager(lowRamDevice: Boolean, totalRamMb: Long) {
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.isLowRamDevice } returns lowRamDevice
        every { activityManager.getMemoryInfo(any()) } answers {
            firstArg<ActivityManager.MemoryInfo>().totalMem = totalRamMb * 1024 * 1024
        }
    }
}
