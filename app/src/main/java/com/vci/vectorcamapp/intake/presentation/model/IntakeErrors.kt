package com.vci.vectorcamapp.intake.presentation.model

import com.vci.vectorcamapp.intake.domain.util.FormValidationError

data class IntakeErrors(
    val collector: FormValidationError?,
    val district: FormValidationError?,
    val villageName: FormValidationError?,
    val houseNumber: FormValidationError?,
    val llinType: FormValidationError?,
    val llinBrand: FormValidationError?,
    val collectionDate: FormValidationError?,
    val collectionMethod: FormValidationError?,
    val specimenCondition: FormValidationError?,
    val monthsSinceIrs: FormValidationError?,
    val numLlinsAvailable: FormValidationError?,
    val numPeopleSleptUnderLlin: FormValidationError?,
    val numPeopleSleptInHouse: FormValidationError?,
    val locationTypeSiteSelections: Map<Int, FormValidationError?>
)
