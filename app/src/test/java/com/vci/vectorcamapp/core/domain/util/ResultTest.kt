package com.vci.vectorcamapp.core.domain.util

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import org.junit.Test

class ResultTest {

    @Test
    fun successHelpers_exposeDataAndSkipErrorCallback() {
        val result: Result<Int, RoomDbError> = Result.Success(4)
        var successValue: Int? = null
        var errorCalled = false

        val mapped = result
            .onError { errorCalled = true }
            .onSuccess { successValue = it }
            .map { it * 2 }

        assertThat(result.successOrNull()).isEqualTo(4)
        assertThat(result.errorOrNull()).isNull()
        assertThat(successValue).isEqualTo(4)
        assertThat(errorCalled).isFalse()
        assertThat(mapped).isEqualTo(Result.Success(8))
        assertThat(result.asEmptyDataResult()).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun errorHelpers_exposeErrorAndSkipSuccessCallback() {
        val result: Result<Int, RoomDbError> = Result.Error(RoomDbError.NO_ROWS_AFFECTED)
        var successCalled = false
        var error: RoomDbError? = null

        val mapped = result
            .onSuccess { successCalled = true }
            .onError { error = it }
            .map { it * 2 }

        assertThat(result.successOrNull()).isNull()
        assertThat(result.errorOrNull()).isEqualTo(RoomDbError.NO_ROWS_AFFECTED)
        assertThat(successCalled).isFalse()
        assertThat(error).isEqualTo(RoomDbError.NO_ROWS_AFFECTED)
        assertThat(mapped).isEqualTo(Result.Error(RoomDbError.NO_ROWS_AFFECTED))
        assertThat(result.asEmptyDataResult()).isEqualTo(Result.Error(RoomDbError.NO_ROWS_AFFECTED))
    }
}
