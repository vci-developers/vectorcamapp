package com.vci.vectorcamapp.ui.extensions

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep

fun MetadataUploadStep.displayText(context: Context): String {
    val resId = when (this) {
        MetadataUploadStep.DEVICE -> R.string.complete_session_upload_step_device
        MetadataUploadStep.SESSION -> R.string.complete_session_upload_step_session
        MetadataUploadStep.SESSION_UNITS -> R.string.complete_session_upload_step_session_units
        MetadataUploadStep.FORM_ANSWERS -> R.string.complete_session_upload_step_form_answers
        MetadataUploadStep.SURVEILLANCE_FORM -> R.string.complete_session_upload_step_surveillance_form
        MetadataUploadStep.SPECIMENS -> R.string.complete_session_upload_step_specimens
        MetadataUploadStep.SPECIMEN_IMAGES -> R.string.complete_session_upload_step_specimen_images
    }
    return context.getString(resId)
}
