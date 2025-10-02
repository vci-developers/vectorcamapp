package com.vci.vectorcamapp.settings.presentation

import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Program

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
        country = ""
    ),
)
