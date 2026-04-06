package com.vci.vectorcamapp.imaging.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateSpecimenIdUseCaseTest {

    private lateinit var useCase: ValidateSpecimenIdUseCase

    @Before
    fun setUp() {
        useCase = ValidateSpecimenIdUseCase()
    }

    // region a - Basic format validation (no auto-correct)

    @Test
    fun a01_validId_noAutoCorrect_returnsSuccess() {
        val result = useCase("ABC123", shouldAutoCorrect = false)
        assertTrue(result is Result.Success)
        assertEquals("ABC123", (result as Result.Success).data)
    }

    @Test
    fun a02_tooShort_returnsError() {
        val result = useCase("AB12", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a03_tooLong_returnsError() {
        val result = useCase("ABCD1234", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a04_containsSpecialCharacters_returnsError() {
        val result = useCase("AB!123", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a05_lettersInDigitPositions_noAutoCorrect_returnsError() {
        val result = useCase("ABCABC", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a06_digitsInLetterPositions_noAutoCorrect_returnsError() {
        val result = useCase("123456", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a07_blankInput_returnsError() {
        val result = useCase("", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun a08_whitespaceOnlyInput_returnsError() {
        val result = useCase("      ", shouldAutoCorrect = false)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    // endregion

    // region b - Input normalisation (trim / spaces / lowercase)

    @Test
    fun b01_leadingTrailingWhitespace_isNormalized() {
        val result = useCase("  ABC123  ", shouldAutoCorrect = false)
        assertTrue(result is Result.Success)
        assertEquals("ABC123", (result as Result.Success).data)
    }

    @Test
    fun b02_internalSpaces_areRemoved() {
        val result = useCase("A B C 1 2 3", shouldAutoCorrect = false)
        assertTrue(result is Result.Success)
        assertEquals("ABC123", (result as Result.Success).data)
    }

    @Test
    fun b03_lowercase_isConvertedToUppercase() {
        val result = useCase("abc123", shouldAutoCorrect = false)
        assertTrue(result is Result.Success)
        assertEquals("ABC123", (result as Result.Success).data)
    }

    // endregion

    // region c - Auto-correct letter positions (index 0-2: digits corrected to look-alike letters)

    @Test
    fun c01_digitZeroInLetterPosition_correctedToO() {
        // "0BC123" → autocorrect "0" to "O" → "OBC123"
        val result = useCase("0BC123", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("OBC123", (result as Result.Success).data)
    }

    @Test
    fun c02_digitOneInLetterPosition_correctedToI() {
        val result = useCase("1BC123", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("IBC123", (result as Result.Success).data)
    }

    @Test
    fun c03_digitFiveInLetterPosition_correctedToS() {
        val result = useCase("5BC123", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("SBC123", (result as Result.Success).data)
    }

    @Test
    fun c04_allThreeLetterPositionsCorrected() {
        // "012123" → "OIZ123"
        val result = useCase("012123", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("OIZ123", (result as Result.Success).data)
    }

    // endregion

    // region d - Auto-correct digit positions (index 3-5: letters corrected to look-alike digits)

    @Test
    fun d01_letterOInDigitPosition_correctedToZero() {
        val result = useCase("ABCO23", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("ABC023", (result as Result.Success).data)
    }

    @Test
    fun d02_letterIInDigitPosition_correctedToOne() {
        val result = useCase("ABCI23", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("ABC123", (result as Result.Success).data)
    }

    @Test
    fun d03_letterSInDigitPosition_correctedToFive() {
        val result = useCase("ABCS23", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("ABC523", (result as Result.Success).data)
    }

    @Test
    fun d04_allThreeDigitPositionsCorrected() {
        // "ABCOIZ" → "ABC012"
        val result = useCase("ABCOIZ", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("ABC012", (result as Result.Success).data)
    }

    // endregion

    // region e - Auto-correct combined letter and digit positions

    @Test
    fun e01_mixedCorrectionBothSides() {
        // "0BCOIZ" → letter side: "0" → "O"; digit side: "O"→"0", "I"→"1", "Z"→"2"
        val result = useCase("0BCOIZ", shouldAutoCorrect = true)
        assertTrue(result is Result.Success)
        assertEquals("OBC012", (result as Result.Success).data)
    }

    // endregion

    // region f - Auto-correct still invalid after correction

    @Test
    fun f01_uncorrectableId_stillReturnsError() {
        // "AB!123" - contains special char, length-check catches this first
        val result = useCase("AB!123", shouldAutoCorrect = true)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    @Test
    fun f02_allDigitsInLetterPositionWithNoMapping_remainsInvalid() {
        // Even with autocorrect, after correction "ABZ123" won't happen from input "ABZ123"
        // Let's use "ABCABC" - digits-only in positions 3-5 corrected: "A"→"4", "B"→"8", "C"→ no digit mapping → stays "C" → invalid
        val result = useCase("ABCABC", shouldAutoCorrect = true)
        assertTrue(result is Result.Error)
        assertEquals(ImagingError.INVALID_SPECIMEN_ID, (result as Result.Error).error)
    }

    // endregion
}
