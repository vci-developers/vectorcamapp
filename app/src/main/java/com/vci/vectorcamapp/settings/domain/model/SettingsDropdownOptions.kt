package com.vci.vectorcamapp.settings.domain.model

object SettingsDropdownOptions {

    enum class CollectorTitleOption(val label: String) {
        TECHNICIAN("Technician"),
        BIOLOGIST_MICROBIOLOGIST("Biologist/Microbiologist"),
        RESEARCHER("Researcher"),
        OTHER("Other")
    }
}
