package com.vci.vectorcamapp.core.domain.util.room

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class RoomDbError : Error {
    CONSTRAINT_VIOLATION,
    UNKNOWN_ERROR;

    override fun toString(context: Context): String {
        val resId = when(this) {
            CONSTRAINT_VIOLATION -> R.string.roomdb_error_constraint_violation
            UNKNOWN_ERROR -> R.string.roomdb_error_unknown_error
        }
        return context.getString(resId)
    }
}
