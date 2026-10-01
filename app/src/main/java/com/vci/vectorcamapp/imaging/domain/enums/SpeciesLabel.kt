package com.vci.vectorcamapp.imaging.domain.enums

enum class SpeciesLabel(val wireValue: String) {
    ANOPHELES_FUNESTUS("Anopheles funestus"),
    ANOPHELES_GAMBIAE("Anopheles gambiae"),
    ANOPHELES_OTHER("Anopheles other"),
    CULEX("Culex"),
    AEDES("Aedes"),
    MANSONIA("Mansonia"),
    NON_MOSQUITO("Non-Mosquito");

    companion object {
        fun fromWireValue(value: String?): SpeciesLabel? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}
