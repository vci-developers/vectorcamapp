package com.vci.vectorcamapp.imaging.domain.enums

enum class AbdomenStatusLabel(val wireValue: String) {
    UNFED("Unfed"),
    FULLY_FED("Fully Fed"),
    GRAVID("Gravid");

    companion object {
        fun fromWireValue(value: String?): AbdomenStatusLabel? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}
