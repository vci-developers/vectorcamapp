package com.vci.vectorcamapp.core.domain.util.room

import com.vci.vectorcamapp.core.domain.util.Error

enum class RoomDbError : Error {
    DUPLICATE_SPECIMEN_ID,
    DUPLICATE_BOUNDING_BOX_ID,
    UNKNOWN_ERROR
}
