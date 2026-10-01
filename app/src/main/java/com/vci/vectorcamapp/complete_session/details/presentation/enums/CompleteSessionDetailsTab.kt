package com.vci.vectorcamapp.complete_session.details.presentation.enums

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R

enum class CompleteSessionDetailsTab(@StringRes val labelResId: Int) {
    SESSION_FORM(R.string.complete_session_tab_form),
    SESSION_SPECIMENS(R.string.complete_session_tab_specimens)
}
