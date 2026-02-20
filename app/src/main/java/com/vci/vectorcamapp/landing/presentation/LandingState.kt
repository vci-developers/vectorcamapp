package com.vci.vectorcamapp.landing.presentation

import com.vci.vectorcamapp.core.domain.model.Program

data class LandingState(
    val isLoading: Boolean = true,
    val enrolledProgram: Program = Program(
        id = -1,
        name = "",
        country = ""
    ),
    val showResumeDialog: Boolean = false,
    val incompleteSessionsCount: Int = 0,
    /** Current session localId (UUID) for CrashyContext when a session is in progress. */
    val currentSessionId: String? = null,
    /** Current site id for CrashyContext when a session is in progress. */
    val currentSiteId: String? = null
)
