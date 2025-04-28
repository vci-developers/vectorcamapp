package com.vci.vectorcamapp.core.presentation.util.room

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError

fun RoomDbError.toString(context: Context) : String {
    val resId = when(this) {
        RoomDbError.DUPLICATE_SPECIMEN_ID -> R.string.roomdb_error_duplicate_specimen_id
        RoomDbError.DUPLICATE_BOUNDING_BOX_ID -> R.string.roomdb_error_duplicate_bounding_box_id
        RoomDbError.UNKNOWN_ERROR -> R.string.roomdb_error_unknown_error
    }
    return context.getString(resId)
}
