package com.vci.vectorcamapp.core.presentation.util.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppLocaleManagerImplementationTest {

    private val manager = AppLocaleManagerImplementation()

    @Before
    fun setUp() {
        mockkStatic(AppCompatDelegate::class)
        mockkStatic(LocaleListCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
        unmockkStatic(LocaleListCompat::class)
    }

    @Test
    fun getCurrentLanguage_readsApplicationLocales() {
        val locales = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns locales
        every { locales.toLanguageTags() } returns "es-MX"

        assertThat(manager.getCurrentLanguage()).isEqualTo(SupportedLanguage.SPANISH)
    }

    @Test
    fun setLanguage_appliesLanguageTag() {
        val locales = mockk<LocaleListCompat>()
        every { LocaleListCompat.forLanguageTags("fr") } returns locales
        every { AppCompatDelegate.setApplicationLocales(any()) } returns Unit

        manager.setLanguage(SupportedLanguage.FRENCH)

        verify { AppCompatDelegate.setApplicationLocales(locales) }
    }
}
