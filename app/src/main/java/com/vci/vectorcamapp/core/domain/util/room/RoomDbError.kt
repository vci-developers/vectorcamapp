package com.vci.vectorcamapp.core.domain.util.room

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class RoomDbError(@StringRes override val messageResId: Int) : Error {
    CONSTRAINT_VIOLATION(R.string.roomdb_error_constraint_violation),
    UNKNOWN_ERROR(R.string.roomdb_error_unknown_error);
}
