package com.vci.vectorcamapp.core.presentation.util.locale

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SupportedLanguageTest {

    @Test
    fun fromTag_matchesPrefixIgnoringCase() {
        assertThat(SupportedLanguage.fromTag(null)).isEqualTo(SupportedLanguage.ENGLISH)
        assertThat(SupportedLanguage.fromTag("")).isEqualTo(SupportedLanguage.ENGLISH)
        assertThat(SupportedLanguage.fromTag("en-US")).isEqualTo(SupportedLanguage.ENGLISH)
        assertThat(SupportedLanguage.fromTag("ES")).isEqualTo(SupportedLanguage.SPANISH)
        assertThat(SupportedLanguage.fromTag("fr-FR")).isEqualTo(SupportedLanguage.FRENCH)
        assertThat(SupportedLanguage.fromTag("de")).isEqualTo(SupportedLanguage.DEFAULT)
    }
}
