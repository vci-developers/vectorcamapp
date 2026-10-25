package com.vci.vectorcamapp.core.presentation.components.camera
import com.google.common.truth.Truth.assertThat
import org.junit.Test
class ScanValueSelectorTest {
    private val validId: (String) -> String? = { value ->
        value.takeIf { it.startsWith("KE") && it.length == 6 }
    }
    @Test
    fun barcodeWinsWhenTextDisagrees() {
        assertThat(selectScanValue(listOf("KE1234"), "KE5678", validId, validId))
            .isEqualTo("KE1234")
    }
    @Test
    fun textIsUsedWhenNoValidBarcodeExists() {
        assertThat(selectScanValue(listOf("invalid"), "KE5678", validId, validId))
            .isEqualTo("KE5678")
        assertThat(selectScanValue(emptyList(), "KE5678", validId, validId))
            .isEqualTo("KE5678")
    }
    @Test
    fun invalidOrDisabledTextProducesNoResult() {
        assertThat(selectScanValue(emptyList(), "invalid", validId, validId)).isNull()
        assertThat(selectScanValue(emptyList(), "KE5678", validId, null)).isNull()
        assertThat(selectScanValue(emptyList(), null, validId, validId)).isNull()
    }
}