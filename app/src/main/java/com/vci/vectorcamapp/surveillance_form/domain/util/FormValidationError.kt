package com.vci.vectorcamapp.surveillance_form.domain.util

import com.vci.vectorcamapp.core.domain.util.Error

enum class FormValidationError : Error {
    BLANK_COUNTRY,
    BLANK_DISTRICT,
    BLANK_HEALTH_CENTER,
    BLANK_SENTINEL_SITE,
    BLANK_HOUSEHOLD_NUMBER,
    FUTURE_COLLECTION_DATE,
    BLANK_COLLECTION_METHOD,
    BLANK_COLLECTOR_NAME,
    BLANK_COLLECTOR_TITLE,
    BLANK_LLIN_TYPE,
    BLANK_LLIN_BRAND,
}
