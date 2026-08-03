package com.vci.vectorcamapp.core.presentation.util.locale

interface AppLocaleManager {
    fun getCurrentLanguage(): SupportedLanguage
    fun setLanguage(language: SupportedLanguage)
}
