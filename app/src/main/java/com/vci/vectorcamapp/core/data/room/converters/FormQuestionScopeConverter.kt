package com.vci.vectorcamapp.core.data.room.converters

import androidx.room.TypeConverter
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope

class FormQuestionScopeConverter {
    @TypeConverter
    fun fromFormQuestionScope(value: FormQuestionScope?): String? = value?.name

    @TypeConverter
    fun toFormQuestionScope(value: String?): FormQuestionScope? =
        value?.let { FormQuestionScope.valueOf(it) }
}