package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntakeValidationUseCasesTest {

    // region ValidateCollectorUseCase

    private lateinit var validateCollector: ValidateCollectorUseCase

    @Before
    fun setUp() {
        validateCollector = ValidateCollectorUseCase()
    }

    @Test
    fun collector_a01_validNameAndTitle_returnsSuccess() {
        val result = validateCollector("Alice", "Doctor")
        assertTrue(result is Result.Success)
    }

    @Test
    fun collector_a02_blankName_returnsError() {
        val result = validateCollector("", "Doctor")
        assertEquals(FormValidationError.BLANK_COLLECTOR, (result as Result.Error).error)
    }

    @Test
    fun collector_a03_blankTitle_returnsError() {
        val result = validateCollector("Alice", "")
        assertEquals(FormValidationError.BLANK_COLLECTOR, (result as Result.Error).error)
    }

    @Test
    fun collector_a04_bothBlank_returnsError() {
        val result = validateCollector("", "")
        assertEquals(FormValidationError.BLANK_COLLECTOR, (result as Result.Error).error)
    }

    @Test
    fun collector_a05_whitespaceOnly_isConsideredBlank() {
        val result = validateCollector("   ", "  ")
        assertEquals(FormValidationError.BLANK_COLLECTOR, (result as Result.Error).error)
    }

    // endregion

    // region ValidateDistrictUseCase

    @Test
    fun district_a01_nonBlankValue_returnsSuccess() {
        val result = ValidateDistrictUseCase()("Kampala")
        assertTrue(result is Result.Success)
    }

    @Test
    fun district_a02_blankValue_returnsError() {
        val result = ValidateDistrictUseCase()("")
        assertEquals(FormValidationError.BLANK_DISTRICT, (result as Result.Error).error)
    }

    @Test
    fun district_a03_whitespaceOnly_returnsError() {
        val result = ValidateDistrictUseCase()("   ")
        assertEquals(FormValidationError.BLANK_DISTRICT, (result as Result.Error).error)
    }

    // endregion

    // region ValidateVillageNameUseCase

    @Test
    fun villageName_a01_nonBlankValue_returnsSuccess() {
        val result = ValidateVillageNameUseCase()("Namuwongo")
        assertTrue(result is Result.Success)
    }

    @Test
    fun villageName_a02_blankValue_returnsError() {
        val result = ValidateVillageNameUseCase()("")
        assertEquals(FormValidationError.BLANK_VILLAGE_NAME, (result as Result.Error).error)
    }

    @Test
    fun villageName_a03_whitespaceOnly_returnsError() {
        val result = ValidateVillageNameUseCase()("   ")
        assertEquals(FormValidationError.BLANK_VILLAGE_NAME, (result as Result.Error).error)
    }

    // endregion

    // region ValidateHouseNumberUseCase

    @Test
    fun houseNumber_a01_nonBlankValue_returnsSuccess() {
        val result = ValidateHouseNumberUseCase()("42A")
        assertTrue(result is Result.Success)
    }

    @Test
    fun houseNumber_a02_blankValue_returnsError() {
        val result = ValidateHouseNumberUseCase()("")
        assertEquals(FormValidationError.BLANK_HOUSE_NUMBER, (result as Result.Error).error)
    }

    // endregion

    // region ValidateLlinTypeUseCase

    @Test
    fun llinType_a01_nonBlankValue_returnsSuccess() {
        val result = ValidateLlinTypeUseCase()("Pyrethroid Only")
        assertTrue(result is Result.Success)
    }

    @Test
    fun llinType_a02_blankValue_returnsError() {
        val result = ValidateLlinTypeUseCase()("")
        assertEquals(FormValidationError.BLANK_LLIN_TYPE, (result as Result.Error).error)
    }

    // endregion

    // region ValidateLlinBrandUseCase

    @Test
    fun llinBrand_a01_nonBlankValue_returnsSuccess() {
        val result = ValidateLlinBrandUseCase()("OLYSET Net")
        assertTrue(result is Result.Success)
    }

    @Test
    fun llinBrand_a02_blankValue_returnsError() {
        val result = ValidateLlinBrandUseCase()("")
        assertEquals(FormValidationError.BLANK_LLIN_BRAND, (result as Result.Error).error)
    }

    // endregion

    // region ValidateCollectionDateUseCase

    @Test
    fun collectionDate_a01_pastDate_returnsSuccess() {
        val pastDate = System.currentTimeMillis() - 86_400_000L // yesterday
        val result = ValidateCollectionDateUseCase()(pastDate)
        assertTrue(result is Result.Success)
    }

    @Test
    fun collectionDate_a02_futureDate_returnsError() {
        val futureDate = System.currentTimeMillis() + 86_400_000L // tomorrow
        val result = ValidateCollectionDateUseCase()(futureDate)
        assertEquals(FormValidationError.FUTURE_COLLECTION_DATE, (result as Result.Error).error)
    }

    // endregion

    // region ValidateNumPeopleSleptInHouseUseCase

    @Test
    fun numPeopleInHouse_a01_zero_returnsSuccess() {
        val result = ValidateNumPeopleSleptInHouseUseCase()(0)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numPeopleInHouse_a02_positive_returnsSuccess() {
        val result = ValidateNumPeopleSleptInHouseUseCase()(5)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numPeopleInHouse_a03_negativeOne_returnsError() {
        val result = ValidateNumPeopleSleptInHouseUseCase()(-1)
        assertEquals(FormValidationError.INVALID_NUM_PEOPLE_SLEPT_IN_HOUSE, (result as Result.Error).error)
    }

    @Test
    fun numPeopleInHouse_a04_veryNegative_returnsError() {
        val result = ValidateNumPeopleSleptInHouseUseCase()(-100)
        assertEquals(FormValidationError.INVALID_NUM_PEOPLE_SLEPT_IN_HOUSE, (result as Result.Error).error)
    }

    // endregion

    // region ValidateNumPeopleSleptUnderLlinUseCase

    @Test
    fun numPeopleUnderLlin_a01_zero_returnsSuccess() {
        val result = ValidateNumPeopleSleptUnderLlinUseCase()(0)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numPeopleUnderLlin_a02_positive_returnsSuccess() {
        val result = ValidateNumPeopleSleptUnderLlinUseCase()(3)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numPeopleUnderLlin_a03_negativeOne_returnsError() {
        val result = ValidateNumPeopleSleptUnderLlinUseCase()(-1)
        assertEquals(FormValidationError.INVALID_NUM_PEOPLE_SLEPT_UNDER_LLIN, (result as Result.Error).error)
    }

    // endregion

    // region ValidateNumLlinsAvailableUseCase

    @Test
    fun numLlins_a01_zero_returnsSuccess() {
        val result = ValidateNumLlinsAvailableUseCase()(0)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numLlins_a02_positive_returnsSuccess() {
        val result = ValidateNumLlinsAvailableUseCase()(10)
        assertTrue(result is Result.Success)
    }

    @Test
    fun numLlins_a03_negativeOne_returnsError() {
        val result = ValidateNumLlinsAvailableUseCase()(-1)
        assertEquals(FormValidationError.INVALID_NUM_LLINS_AVAILABLE, (result as Result.Error).error)
    }

    // endregion

    // region ValidateMonthsSinceIrsUseCase

    @Test
    fun monthsSinceIrs_a01_zero_returnsSuccess() {
        val result = ValidateMonthsSinceIrsUseCase()(0)
        assertTrue(result is Result.Success)
    }

    @Test
    fun monthsSinceIrs_a02_positive_returnsSuccess() {
        val result = ValidateMonthsSinceIrsUseCase()(6)
        assertTrue(result is Result.Success)
    }

    @Test
    fun monthsSinceIrs_a03_negativeOne_returnsError() {
        val result = ValidateMonthsSinceIrsUseCase()(-1)
        assertEquals(FormValidationError.INVALID_MONTHS_SINCE_IRS, (result as Result.Error).error)
    }

    // endregion

    // region ValidateCollectionMethodUseCase

    @Test
    fun collectionMethod_a01_standardMethod_returnsSuccess() {
        val result = ValidateCollectionMethodUseCase()(
            IntakeDropdownOptions.CollectionMethodOption.CDC_LIGHT_TRAP.label
        )
        assertTrue(result is Result.Success)
    }

    @Test
    fun collectionMethod_a02_blank_returnsError() {
        val result = ValidateCollectionMethodUseCase()("")
        assertEquals(FormValidationError.BLANK_COLLECTION_METHOD, (result as Result.Error).error)
    }

    @Test
    fun collectionMethod_a03_otherWithDescription_returnsSuccess() {
        val label = IntakeDropdownOptions.CollectionMethodOption.OTHER.label
        val result = ValidateCollectionMethodUseCase()("$label: Manual sweep")
        assertTrue(result is Result.Success)
    }

    @Test
    fun collectionMethod_a04_otherWithNoDescription_returnsError() {
        val label = IntakeDropdownOptions.CollectionMethodOption.OTHER.label
        val result = ValidateCollectionMethodUseCase()(label)
        assertEquals(FormValidationError.BLANK_COLLECTION_METHOD, (result as Result.Error).error)
    }

    @Test
    fun collectionMethod_a05_otherWithWhitespaceOnlyDescription_returnsError() {
        val label = IntakeDropdownOptions.CollectionMethodOption.OTHER.label
        val result = ValidateCollectionMethodUseCase()("$label   ")
        assertEquals(FormValidationError.BLANK_COLLECTION_METHOD, (result as Result.Error).error)
    }

    // endregion

    // region ValidateSpecimenConditionUseCase

    @Test
    fun specimenCondition_a01_standardCondition_returnsSuccess() {
        val result = ValidateSpecimenConditionUseCase()(
            IntakeDropdownOptions.SpecimenConditionOption.FRESH.label
        )
        assertTrue(result is Result.Success)
    }

    @Test
    fun specimenCondition_a02_blank_returnsError() {
        val result = ValidateSpecimenConditionUseCase()("")
        assertEquals(FormValidationError.BLANK_SPECIMEN_CONDITION, (result as Result.Error).error)
    }

    @Test
    fun specimenCondition_a03_otherWithDescription_returnsSuccess() {
        val label = IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
        val result = ValidateSpecimenConditionUseCase()("$label: Slightly damaged")
        assertTrue(result is Result.Success)
    }

    @Test
    fun specimenCondition_a04_otherWithNoDescription_returnsError() {
        val label = IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
        val result = ValidateSpecimenConditionUseCase()(label)
        assertEquals(FormValidationError.BLANK_SPECIMEN_CONDITION, (result as Result.Error).error)
    }

    @Test
    fun specimenCondition_a05_otherWithWhitespaceOnlyDescription_returnsError() {
        val label = IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
        val result = ValidateSpecimenConditionUseCase()("$label   ")
        assertEquals(FormValidationError.BLANK_SPECIMEN_CONDITION, (result as Result.Error).error)
    }

    // endregion
}
