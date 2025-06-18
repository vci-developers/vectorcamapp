package com.vci.vectorcamapp.registration.presentation

import com.vci.vectorcamapp.core.domain.model.Program

data class RegistrationState(
    val isLoading: Boolean = true,
    val programs: List<Program> = emptyList(),
    val error: String? = null
)