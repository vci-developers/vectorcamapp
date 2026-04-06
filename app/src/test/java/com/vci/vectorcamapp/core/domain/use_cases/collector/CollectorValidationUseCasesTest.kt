package com.vci.vectorcamapp.core.domain.use_cases.collector

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorValidationUseCasesTest {

    // region ValidateCollectorNameUseCase

    @Test
    fun collectorName_a01_nonBlankName_returnsSuccess() {
        val result = ValidateCollectorNameUseCase()("Alice")
        assertTrue(result is Result.Success)
    }

    @Test
    fun collectorName_a02_blankName_returnsError() {
        val result = ValidateCollectorNameUseCase()("")
        assertEquals(CollectorValidationError.BLANK_COLLECTOR_NAME, (result as Result.Error).error)
    }

    @Test
    fun collectorName_a03_whitespaceOnly_returnsError() {
        val result = ValidateCollectorNameUseCase()("   ")
        assertEquals(CollectorValidationError.BLANK_COLLECTOR_NAME, (result as Result.Error).error)
    }

    // endregion

    // region ValidateCollectorTitleUseCase

    @Test
    fun collectorTitle_a01_nonBlankTitle_returnsSuccess() {
        val result = ValidateCollectorTitleUseCase()("Doctor")
        assertTrue(result is Result.Success)
    }

    @Test
    fun collectorTitle_a02_blankTitle_returnsError() {
        val result = ValidateCollectorTitleUseCase()("")
        assertEquals(CollectorValidationError.BLANK_COLLECTOR_TITLE, (result as Result.Error).error)
    }

    @Test
    fun collectorTitle_a03_whitespaceOnly_returnsError() {
        val result = ValidateCollectorTitleUseCase()("   ")
        assertEquals(CollectorValidationError.BLANK_COLLECTOR_TITLE, (result as Result.Error).error)
    }

    // endregion

    // region ValidateCollectorLastTrainedOnUseCase

    @Test
    fun lastTrainedOn_a01_validPastDate_returnsSuccess() {
        val pastDate = System.currentTimeMillis() - 86_400_000L // yesterday
        val result = ValidateCollectorLastTrainedOnUseCase()(pastDate)
        assertTrue(result is Result.Success)
    }

    @Test
    fun lastTrainedOn_a02_futureDate_returnsError() {
        val futureDate = System.currentTimeMillis() + 86_400_000L // tomorrow
        val result = ValidateCollectorLastTrainedOnUseCase()(futureDate)
        assertEquals(
            CollectorValidationError.INVALID_LAST_TRAINED_ON_DATE,
            (result as Result.Error).error
        )
    }

    @Test
    fun lastTrainedOn_a03_zeroTimestamp_returnsError() {
        val result = ValidateCollectorLastTrainedOnUseCase()(0L)
        assertEquals(
            CollectorValidationError.INVALID_LAST_TRAINED_ON_DATE,
            (result as Result.Error).error
        )
    }

    @Test
    fun lastTrainedOn_a04_negativeTimestamp_returnsError() {
        val result = ValidateCollectorLastTrainedOnUseCase()(-1L)
        assertEquals(
            CollectorValidationError.INVALID_LAST_TRAINED_ON_DATE,
            (result as Result.Error).error
        )
    }

    // endregion
}
