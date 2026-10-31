package com.vci.vectorcamapp.core.data.upload.logging

import com.google.common.truth.Truth.assertThat
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UploadFailureReportTest {

    @Test
    fun build_includesSessionEndpointMethodAndBodies() {
        val message = UploadFailureReport.build(
            worker = "MetadataUploadWorker",
            sessionId = "session-1",
            step = "sync_session",
            programId = "4",
            siteId = "9",
            specimenId = "spec-1",
            imageId = "image-1",
            reason = "Session metadata was not uploaded",
            networkError = "SERVER_ERROR",
            exchange = UploadHttpExchange(
                method = "POST",
                endpoint = "https://api.example.com/sessions",
                requestBody = """{"deviceId":7}""",
                statusCode = 500,
                responseBody = """{"error":"database down"}""",
            ),
        )

        assertThat(message).contains("MetadataUploadWorker")
        assertThat(message).contains("session=session-1")
        assertThat(message).contains("step=sync_session")
        assertThat(message).contains("method=POST")
        assertThat(message).contains("endpoint=https://api.example.com/sessions")
        assertThat(message).contains("status=500")
        assertThat(message).contains("""request={"deviceId":7}""")
        assertThat(message).contains("""response={"error":"database down"}""")
        assertThat(message).contains("networkError=SERVER_ERROR")
    }

    @Test
    fun build_describesMissingHttpCall() {
        val message = UploadFailureReport.build(
            worker = "ImageUploadWorker",
            sessionId = null,
            step = null,
            programId = null,
            siteId = null,
            specimenId = null,
            imageId = null,
            reason = "Session id missing",
            networkError = null,
            exchange = null,
        )

        assertThat(message).contains("session=unknown")
        assertThat(message).contains("method=(none)")
        assertThat(message).contains("endpoint=(none)")
        assertThat(message).contains("request=(no http call)")
        assertThat(message).contains("response=(no http response)")
    }

    @Test
    fun exchangeFrom_readsMethodEndpointStatusAndResponseBody() {
        val connection = mockk<HttpURLConnection>()
        every { connection.requestMethod } returns "PATCH"
        every { connection.url } returns URL("https://api.example.com/specimens/3/images/tus/abc")
        every { connection.responseCode } returns 413
        every { connection.errorStream } returns "payload too large".byteInputStream()

        val exchange = UploadFailureReport.exchangeFrom(
            throwable = IOException("unused"),
            connection = connection,
            fallbackMethod = "POST",
            fallbackEndpoint = "https://fallback.example",
            requestBody = "metadata={filemd5=abc} binaryPayload=omitted",
        )

        assertThat(exchange.method).isEqualTo("PATCH")
        assertThat(exchange.endpoint).isEqualTo("https://api.example.com/specimens/3/images/tus/abc")
        assertThat(exchange.statusCode).isEqualTo(413)
        assertThat(exchange.requestBody).contains("binaryPayload=omitted")
        assertThat(exchange.responseBody).isEqualTo("payload too large")
    }

    @Test
    fun exchangeFrom_usesFallbackWhenConnectionCannotBeRead() {
        val connection = mockk<HttpURLConnection>()
        every { connection.requestMethod } throws IOException("closed")
        every { connection.url } throws IOException("closed")
        every { connection.responseCode } throws IOException("closed")
        every { connection.errorStream } throws IOException("closed")

        val exchange = UploadFailureReport.exchangeFrom(
            throwable = IOException("timed out"),
            connection = connection,
            fallbackMethod = "PATCH",
            fallbackEndpoint = "https://api.example.com/specimens/3/images/tus",
            requestBody = null,
        )

        assertThat(exchange.method).isEqualTo("PATCH")
        assertThat(exchange.endpoint).isEqualTo("https://api.example.com/specimens/3/images/tus")
        assertThat(exchange.statusCode).isNull()
        assertThat(exchange.requestBody).isEqualTo("(empty)")
        assertThat(exchange.responseBody).isEqualTo("(unreadable response body: closed)")
    }

    @Test
    fun outgoingContent_logsJsonRequestBody() {
        val content = TextContent(
            text = """{"appVersion":"42(1.4.0)"}""",
            contentType = ContentType.Application.Json,
        )

        assertThat(UploadFailureReport.outgoingContent(content))
            .isEqualTo("""{"appVersion":"42(1.4.0)"}""")
    }
}
