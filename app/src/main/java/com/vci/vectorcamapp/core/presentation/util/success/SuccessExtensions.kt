package com.vci.vectorcamapp.core.presentation.util.success

import android.content.Context
import com.vci.vectorcamapp.R

fun SuccessMessage.toString(context: Context): String {
    val resId = when (this) {
        SuccessMessage.PROGRAM_DATA_DOWNLOADED -> R.string.success_program_data_downloaded
        SuccessMessage.SESSION_CREATED -> R.string.success_session_created
        SuccessMessage.SESSION_DATA_SAVED -> R.string.success_session_data_saved
        SuccessMessage.SESSION_QUEUED_FOR_SUBMISSION -> R.string.success_session_queued_for_submission
    }
    return context.getString(resId)
}
