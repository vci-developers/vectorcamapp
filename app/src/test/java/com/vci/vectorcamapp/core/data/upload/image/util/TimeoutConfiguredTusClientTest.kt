package com.vci.vectorcamapp.core.data.upload.image.util

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.net.HttpURLConnection

class TimeoutConfiguredTusClientTest {

    @Test
    fun prepareConnection_appliesInjectedTimeouts() {
        val connection = mockk<HttpURLConnection>(relaxed = true)
        val client = TimeoutConfiguredTusClient(
            connectTimeoutMs = 5_000,
            readTimeoutMs = 20_000,
        )

        client.prepareConnection(connection)

        verify { connection.connectTimeout = 5_000 }
        verify { connection.readTimeout = 20_000 }
    }
}
