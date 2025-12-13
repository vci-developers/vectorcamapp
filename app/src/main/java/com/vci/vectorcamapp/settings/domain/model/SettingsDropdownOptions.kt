package com.vci.vectorcamapp.settings.domain.model

object SettingsDropdownOptions {

    enum class CollectorTitleOption(val label: String) {
        VECTOR_CONTROL_OFFICER("Vector Control Officer (VCO)"),
        VILLAGE_HEALTH_TEAM("Village Health Team (VHT)"),
        FIELD_OPERATIONS_TEAM("Field Operations Team (FOT)")
    }
}
