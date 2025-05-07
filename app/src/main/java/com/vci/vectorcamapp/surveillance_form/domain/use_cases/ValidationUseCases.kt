package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import javax.inject.Inject

data class ValidationUseCases @Inject constructor(
    val validateCountry: ValidateCountryUseCase,
    val validateDistrict: ValidateDistrictUseCase,
    val validateHealthCenter: ValidateHealthCenterUseCase,
    val validateSentinelSite: ValidateSentinelSiteUseCase,
    val validateHouseholdNumber: ValidateHouseholdNumberUseCase,
    val validateCollectionDate: ValidateCollectionDateUseCase,
    val validateCollectionMethod: ValidateCollectionMethodUseCase,
    val validateCollectorName: ValidateCollectorNameUseCase,
    val validateCollectorTitle: ValidateCollectorTitleUseCase,
    val validateLlinType: ValidateLlinTypeUseCase,
    val validateLlinBrand: ValidateLlinBrandUseCase,
)
