package com.vci.vectorcamapp.core.presentation.util.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

class AppLocaleManagerImplementation @Inject constructor() : AppLocaleManager {

    override fun getCurrentLanguage(): SupportedLanguage =
        SupportedLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    override fun setLanguage(language: SupportedLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
