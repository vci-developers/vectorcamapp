package com.vci.vectorcamapp.incomplete_session.presentation

import java.util.UUID

sealed interface IncompleteSessionAction {
    data class ResumeSession(val sessionId: UUID) : IncompleteSessionAction
    data class DeleteSession(val sessionId: UUID) : IncompleteSessionAction
    data class ConfirmDeleteSession(val sessionId: UUID) : IncompleteSessionAction
    data object DismissDeleteDialog : IncompleteSessionAction
    data object ReturnToLandingScreen : IncompleteSessionAction
    data class UpdateSearchQuery(val searchQuery: String) : IncompleteSessionAction
    data object ShowSearchTooltipDialog : IncompleteSessionAction
    data object HideSearchTooltipDialog : IncompleteSessionAction
}