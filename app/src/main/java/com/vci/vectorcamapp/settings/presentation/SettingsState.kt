package com.vci.vectorcamapp.settings.presentation

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.settings.presentation.model.SettingsErrors

data class SettingsState (
    val device: Device = Device(
        id = -1,
        model = "",
        registeredAt = 0L,
        submittedAt = null
    ),
    val program: Program = Program(
        id = -1,
        name = "",
        country = "",
        formVersion = null
    ),
    val settingsErrors: SettingsErrors = SettingsErrors(
        collectorName = null,
        collectorTitle = null,
        collectorLastTrainedOn = null
    ),
    val collectors: List<Collector> = emptyList(),
    val selectedCollector: Collector? = null,
    val similarCollector: Collector? = null,
    val isEditCollectorDialogVisible: Boolean = false,
    val isDeleteCollectorDialogVisible: Boolean = false
)
