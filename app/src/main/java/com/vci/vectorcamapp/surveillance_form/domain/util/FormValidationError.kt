package com.vci.vectorcamapp.surveillance_form.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class FormValidationError : Error {
    BLANK_COLLECTOR_TITLE,
    BLANK_COLLECTOR_NAME,
    BLANK_DISTRICT,
    BLANK_SENTINEL_SITE,
    BLANK_HOUSE_NUMBER,
    BLANK_LLIN_TYPE,
    BLANK_LLIN_BRAND,
    FUTURE_COLLECTION_DATE,
    BLANK_COLLECTION_METHOD,
    BLANK_SPECIMEN_CONDITION;

    override fun toString(context: Context): String {
        val resId = when(this) {
            BLANK_COLLECTOR_TITLE -> R.string.form_validation_error_blank_collector_title
            BLANK_COLLECTOR_NAME -> R.string.form_validation_error_blank_collector_name
            BLANK_DISTRICT -> R.string.form_validation_error_blank_district
            BLANK_SENTINEL_SITE -> R.string.form_validation_error_blank_sentinel_site
            BLANK_HOUSE_NUMBER -> R.string.form_validation_error_blank_house_number
            BLANK_LLIN_TYPE -> R.string.form_validation_error_blank_llin_type
            BLANK_LLIN_BRAND -> R.string.form_validation_error_blank_llin_brand
            FUTURE_COLLECTION_DATE -> R.string.form_validation_error_future_collection_date
            BLANK_COLLECTION_METHOD -> R.string.form_validation_error_blank_collection_method
            BLANK_SPECIMEN_CONDITION -> R.string.form_validation_error_blank_specimen_condition
        }
        return context.getString(resId)
    }
}
