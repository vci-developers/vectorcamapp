package com.vci.vectorcamapp.imaging.domain.enums

enum class SexLabel(val wireValue: String) {
    FEMALE("Female"),
    MALE("Male");

    companion object {
        fun fromWireValue(value: String?): SexLabel? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}
