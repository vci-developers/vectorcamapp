package com.vci.vectorcamapp.registration.domain.model

object RegistrationDropdownOptions {

    enum class CollectorTitleOption(val label: String) {
        TECHNICIAN("Technician"),
        BIOLOGIST_MICROBIOLOGIST("Biologist/Microbiologist"),
        RESEARCHER("Researcher"),
        OTHER("Other")
    }
}
