package com.vci.vectorcamapp.intake.domain.use_cases

import javax.inject.Inject

data class IntakeValidationUseCases @Inject constructor(
    val validateCollector: ValidateCollectorUseCase,
    val validateDistrict: ValidateDistrictUseCase,
    val validateVillageName: ValidateVillageNameUseCase,
    val validateHouseNumber: ValidateHouseNumberUseCase,
    val validateLlinType: ValidateLlinTypeUseCase,
    val validateLlinBrand: ValidateLlinBrandUseCase,
    val validateCollectionDate: ValidateCollectionDateUseCase,
    val validateCollectionMethod: ValidateCollectionMethodUseCase,
    val validateSpecimenCondition: ValidateSpecimenConditionUseCase,
    val validateNumPeopleSleptInHouse: ValidateNumPeopleSleptInHouseUseCase,
    val validateMonthsSinceIrs: ValidateMonthsSinceIrsUseCase,
    val validateNumLlinsAvailable: ValidateNumLlinsAvailableUseCase,
    val validateNumPeopleSleptUnderLlin: ValidateNumPeopleSleptUnderLlinUseCase,
)
