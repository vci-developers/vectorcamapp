package com.vci.vectorcamapp.registration.domain.constants

object ProgramRegistrationPasswords {
    private val passwordsByProgramId = mapOf(
        // Production (api.vectorcam.org)
        1 to "nmedug01",       // National Malaria Elimination Division – Uganda
        2 to "datacol2",       // Data Collection – Colombia
        3 to "pilogh03",       // Pilot Field Study – Ghana
        4 to "kemriken4",      // KEMRI - AnoSTEP – Kenya

        // Staging (test.api.vectorcam.org)
        7 to "nmedug07",       // National Malaria Elimination Division – Uganda
        8 to "testprog8",      // TEST
        9 to "pilogh09",       // Pilot Field Study – Ghana
        10 to "anokeny10",     // Kenya ANOSTEP – Kenya
        11 to "testpro11",     // TEST 2
    )

    fun passwordForProgram(programId: Int): String? = passwordsByProgramId[programId]
}
