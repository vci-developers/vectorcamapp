package com.vci.vectorcamapp.imaging.domain.use_cases
import com.google.common.truth.Truth.assertThat
import org.junit.Test
class ExtractPrintedSpecimenIdTest {
    @Test
    fun extractsIdFromSurroundingLabelText() {
        assertThat(extractPrintedSpecimenId("Specimen\nKE1234\nKenya")).isEqualTo("KE1234")
        assertThat(extractPrintedSpecimenId("(KEAB12)")).isEqualTo("KEAB12")
    }
    @Test
    fun rejectsWrongPrefixLengthAndEmbeddedIds() {
        listOf("ke1234", "KE123", "KE12345", "XKE1234", "nothing", "KE 1234").forEach {
            assertThat(extractPrintedSpecimenId(it)).isNull()
        }
    }
    @Test
    fun refusesAmbiguousIdsButAllowsRepeatedSameId() {
        assertThat(extractPrintedSpecimenId("KE1234 KE5678")).isNull()
        assertThat(extractPrintedSpecimenId("KE1234\nKE1234")).isEqualTo("KE1234")
    }
}