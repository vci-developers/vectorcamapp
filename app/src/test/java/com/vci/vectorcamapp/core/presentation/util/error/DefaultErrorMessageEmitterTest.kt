package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.material3.SnackbarDuration
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultErrorMessageEmitterTest {

    private val emitter = DefaultErrorMessageEmitter()

    @Test
    fun emit_publishesErrorAndDuration() = runTest {
        emitter.errors.test {
            emitter.emit(NetworkError.NO_INTERNET, SnackbarDuration.Short)

            val published = awaitItem()
            assertThat(published.error).isEqualTo(NetworkError.NO_INTERNET)
            assertThat(published.duration).isEqualTo(SnackbarDuration.Short)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emit_dropsConsecutiveDuplicate() = runTest {
        emitter.errors.test {
            emitter.emit(NetworkError.SERVER_ERROR)
            awaitItem()

            emitter.emit(NetworkError.SERVER_ERROR)
            expectNoEvents()

            emitter.emit(NetworkError.NO_INTERNET)
            assertThat(awaitItem().error).isEqualTo(NetworkError.NO_INTERNET)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearLastMessage_allowsTheSameErrorAgain() = runTest {
        emitter.errors.test {
            emitter.emit(NetworkError.CONFLICT)
            awaitItem()

            emitter.clearLastMessage()
            emitter.emit(NetworkError.CONFLICT)

            assertThat(awaitItem().error).isEqualTo(NetworkError.CONFLICT)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
