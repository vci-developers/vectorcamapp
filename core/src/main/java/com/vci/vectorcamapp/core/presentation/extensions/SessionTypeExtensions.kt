package com.vci.vectorcamapp.core.presentation.extensions

import android.content.Context
import com.vci.vectorcamapp.core.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType

fun SessionType.displayText(context: Context): String {
    val resId = when (this) {
        SessionType.SURVEILLANCE -> R.string.session_type_surveillance
        SessionType.DATA_COLLECTION -> R.string.session_type_data_collection
    }
    return context.getString(resId)
}
