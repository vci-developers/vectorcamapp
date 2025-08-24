package com.vci.vectorcamapp.complete_session.details.presentation

import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab

sealed interface CompleteSessionDetailsAction {
    data object ReturnToCompleteSessionListScreen : CompleteSessionDetailsAction
    data class ChangeSelectedTab(val selectedTab: CompleteSessionDetailsTab) : CompleteSessionDetailsAction
    data class UpdateQuery(val value: String) : CompleteSessionDetailsAction
    data object ExecuteQuery : CompleteSessionDetailsAction
}
