package com.vci.vectorcamapp.core.domain.util.room

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class RoomDbError : Error {
    DUPLICATE_SPECIMEN_ID,
    DUPLICATE_BOUNDING_BOX_ID,
    UNKNOWN_ERROR;

    override fun toString(context: Context): String {
        val resId = when(this) {
            DUPLICATE_SPECIMEN_ID -> R.string.roomdb_error_duplicate_specimen_id
            DUPLICATE_BOUNDING_BOX_ID -> R.string.roomdb_error_duplicate_bounding_box_id
            UNKNOWN_ERROR -> R.string.roomdb_error_unknown_error
        }
        return context.getString(resId)
    }
}
