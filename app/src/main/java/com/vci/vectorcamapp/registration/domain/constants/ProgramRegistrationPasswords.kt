package com.vci.vectorcamapp.registration.domain.constants

object ProgramRegistrationPasswords {
    private val passwordsByProgramId = mapOf(
        1 to "program-one-password",
        2 to "program-two-password",
        3 to "program-three-password",
    )

    fun passwordForProgram(programId: Int): String? = passwordsByProgramId[programId]
}
