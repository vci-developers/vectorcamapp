package com.vci.vectorcamapp.core.data.network

import com.google.common.truth.Truth.assertThat
import io.ktor.http.Url
import org.junit.Test

class ApiErrorLoggerTest {

    @Test
    fun route_keepsPathAndQuery() {
        val url = Url("https://api.example.com/devices/7?programId=4")

        assertThat(ApiErrorLogger.route(url)).isEqualTo("/devices/7?programId=4")
    }

    @Test
    fun route_omitsEmptyQuery() {
        val url = Url("https://api.example.com/sessions/abc")

        assertThat(ApiErrorLogger.route(url)).isEqualTo("/sessions/abc")
    }

    @Test
    fun message_includesRouteHttpCodeAndResponse() {
        assertThat(
            ApiErrorLogger.message(
                method = "PUT",
                route = "/devices/7",
                httpCode = 500,
                responseBody = """{"error":"nope"}""",
            )
        ).isEqualTo("""PUT /devices/7 http=500 response={"error":"nope"}""")
    }
}
