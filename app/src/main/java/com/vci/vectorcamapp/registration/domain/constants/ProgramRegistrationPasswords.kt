package com.vci.vectorcamapp.registration.domain.constants

object ProgramRegistrationPasswords {
    private val passwordsByProgramId = mapOf(
        // Production (api.vectorcam.org)
        1 to "nmed-uganda",    // National Malaria Elimination Division – Uganda
        2 to "data-colombia",  // Data Collection – Colombia
        3 to "pilot-ghana",    // Pilot Field Study – Ghana
        4 to "kemri-kenya",    // KEMRI - AnoSTEP – Kenya

        // Staging (test.api.vectorcam.org)
        7 to "nmed-uganda",    // National Malaria Elimination Division – Uganda
        8 to "test-1234",      // TEST
        9 to "pilot-ghana",    // Pilot Field Study – Ghana
        10 to "anostep-kenya", // Kenya ANOSTEP – Kenya
        11 to "test2-1234",    // TEST 2
    )

    fun passwordForProgram(programId: Int): String? = passwordsByProgramId[programId]
}
