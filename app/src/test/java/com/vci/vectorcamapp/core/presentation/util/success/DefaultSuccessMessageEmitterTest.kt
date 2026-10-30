package com.vci.vectorcamapp.core.presentation.util.success

import androidx.compose.material3.SnackbarDuration
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultSuccessMessageEmitterTest {

    private val emitter = DefaultSuccessMessageEmitter()

    @Test
    fun emit_publishesMessageAndDuration() = runTest {
        emitter.messages.test {
            emitter.emit(SuccessMessage.SESSION_CREATED, SnackbarDuration.Short)

            val published = awaitItem()
            assertThat(published.message).isEqualTo(SuccessMessage.SESSION_CREATED)
            assertThat(published.duration).isEqualTo(SnackbarDuration.Short)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emit_dropsConsecutiveDuplicate() = runTest {
        emitter.messages.test {
            emitter.emit(SuccessMessage.SESSION_DATA_RECORDED)
            awaitItem()

            emitter.emit(SuccessMessage.SESSION_DATA_RECORDED)
            expectNoEvents()

            emitter.emit(SuccessMessage.SESSION_CREATED)
            assertThat(awaitItem().message).isEqualTo(SuccessMessage.SESSION_CREATED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearLastMessage_allowsTheSameMessageAgain() = runTest {
        emitter.messages.test {
            emitter.emit(SuccessMessage.PROGRAM_DATA_DOWNLOADED)
            awaitItem()

            emitter.clearLastMessage()
            emitter.emit(SuccessMessage.PROGRAM_DATA_DOWNLOADED)

            assertThat(awaitItem().message).isEqualTo(SuccessMessage.PROGRAM_DATA_DOWNLOADED)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
