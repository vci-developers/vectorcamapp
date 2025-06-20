package com.vci.vectorcamapp.registration.domain.enums

import com.vci.vectorcamapp.surveillance_form.domain.enums.DropdownOption

data class ProgramOption(
    val id: Int,
    override val label: String
) : DropdownOption