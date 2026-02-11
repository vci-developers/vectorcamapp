package com.vci.vectorcamapp.registration.presentation

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.registration.presentation.model.RegistrationErrors
import java.util.UUID

data class RegistrationState(
    val isConnectedToInternet: Boolean = false,
    val isLoadingPrograms: Boolean = false,
    val isLoadingSites: Boolean = false,
    val programs: List<Program> = emptyList(),
    val selectedProgram: Program? = null,
    val collector: Collector = Collector(
        id = UUID.randomUUID(),
        name = "",
        title = "",
        lastTrainedOn = System.currentTimeMillis()
    ),
    val registrationErrors: RegistrationErrors = RegistrationErrors(
        collectorName = null,
        collectorTitle = null
    )
)
