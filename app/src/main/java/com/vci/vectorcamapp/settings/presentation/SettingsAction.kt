package com.vci.vectorcamapp.settings.presentation

import com.vci.vectorcamapp.core.domain.model.Collector

interface SettingsAction {
    data object ReturnToLandingScreen : SettingsAction
    data object StartNewDataCollectionSession : SettingsAction
    data object ShowAddCollectorDialog : SettingsAction
    data class ShowEditCollectorDialog(val collector: Collector) : SettingsAction
    data object DismissCollectorDialog : SettingsAction
    data class EnterCollectorName(val name: String) : SettingsAction
    data class EnterCollectorTitle(val title: String) : SettingsAction
    data class EnterCollectorLastTrainedOn(val lastTrainedOn: Long) : SettingsAction
    data object SaveCollector : SettingsAction
    data object ShowDeleteCollectorDialog : SettingsAction
    data object DismissDeleteCollectorDialog : SettingsAction
    data object ConfirmDeleteCollector : SettingsAction
}
