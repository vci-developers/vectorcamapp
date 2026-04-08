package com.vci.vectorcamapp.settings.domain.util

import com.vci.vectorcamapp.core.domain.util.Error

enum class SettingsError : Error {
    COLLECTOR_SAVE_FAILED,
    COLLECTOR_DELETION_FAILED,
    DATA_SYNC_FAILED,
    DATA_SYNC_IN_PROGRESS_SESSION_EXIST
}
