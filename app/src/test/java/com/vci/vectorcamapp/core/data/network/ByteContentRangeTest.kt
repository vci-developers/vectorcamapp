package com.vci.vectorcamapp.core.data.network

import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpStatusCode
import org.junit.Test

class ByteContentRangeTest {

    @Test
    fun startByte_readsOffsetFromContentRange() {
        assertThat(ByteContentRange.startByte("bytes 1024-2047/4096")).isEqualTo(1024L)
        assertThat(ByteContentRange.startByte("bytes 0-10/*")).isEqualTo(0L)
        assertThat(ByteContentRange.startByte("Bytes 8-8/9")).isEqualTo(8L)
    }

    @Test
    fun startByte_rejectsMissingOrNonByteRanges() {
        assertThat(ByteContentRange.startByte(null)).isNull()
        assertThat(ByteContentRange.startByte("")).isNull()
        assertThat(ByteContentRange.startByte("bytes */4096")).isNull()
    }

    @Test
    fun resumeDisposition_appendsOnlyWhenRangeStartsAtRequest() {
        assertThat(
            resumeDisposition(HttpStatusCode.PartialContent.value, "bytes 500-999/1000", 500L)
        ).isEqualTo(ResumeDisposition.APPEND)
    }

    @Test
    fun resumeDisposition_restartsWhenServerSendsTheFileFromZero() {
        assertThat(
            resumeDisposition(HttpStatusCode.OK.value, null, 500L)
        ).isEqualTo(ResumeDisposition.RESTART)
        assertThat(
            resumeDisposition(HttpStatusCode.PartialContent.value, "bytes 0-999/1000", 500L)
        ).isEqualTo(ResumeDisposition.RESTART)
    }

    @Test
    fun resumeDisposition_rejectsGapOrMissingRange() {
        assertThat(
            resumeDisposition(HttpStatusCode.PartialContent.value, "bytes 800-999/1000", 500L)
        ).isEqualTo(ResumeDisposition.REJECT)
        assertThat(
            resumeDisposition(HttpStatusCode.PartialContent.value, null, 500L)
        ).isEqualTo(ResumeDisposition.REJECT)
    }

    @Test
    fun resumeDisposition_fullDownloadDoesNotRequireARange() {
        assertThat(
            resumeDisposition(HttpStatusCode.OK.value, null, 0L)
        ).isEqualTo(ResumeDisposition.APPEND)
    }
}
