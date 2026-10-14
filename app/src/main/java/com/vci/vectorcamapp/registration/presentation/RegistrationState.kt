package com.vci.vectorcamapp.registration.presentation

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.registration.domain.util.RegistrationError
import com.vci.vectorcamapp.registration.presentation.model.RegistrationErrors
import java.util.UUID

data class RegistrationState(
    val isConnectedToInternet: Boolean = false,
    val isLoadingPrograms: Boolean = false,
    val isLoading: Boolean = false,
    /**
     * Loading sub-phase after access-code success.
     * Null when not loading.
     */
    val loadingPhase: RegistrationLoadingPhase? = null,
    /** 0f–1f while [loadingPhase] is [RegistrationLoadingPhase.DOWNLOADING_MODEL]. */
    val modelDownloadProgress: Float = 0f,
    val modelDownloadBytes: Long = 0L,
    val modelDownloadTotalBytes: Long = 0L,
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
    ),
    val isProgramAccessCodeDialogVisible: Boolean = false,
    val programAccessCodeInput: String = "",
    val programAccessCodeError: RegistrationError? = null
)
