package com.vci.vectorcamapp.core.presentation.util.locale

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R

enum class SupportedLanguage(
    val tag: String,
    @StringRes val displayNameResId: Int
) {
    ENGLISH("en", R.string.language_english),
    SPANISH("es", R.string.language_spanish),
    FRENCH("fr", R.string.language_french);

    companion object {
        val DEFAULT = ENGLISH

        fun fromTag(tag: String?): SupportedLanguage =
            entries.firstOrNull { tag?.startsWith(it.tag, ignoreCase = true) == true } ?: DEFAULT
    }
}
