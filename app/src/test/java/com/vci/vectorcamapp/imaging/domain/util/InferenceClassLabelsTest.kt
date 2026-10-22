package com.vci.vectorcamapp.imaging.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InferenceClassLabelsTest {

    private val bundled = listOf("Anopheles funestus", "Culex", "Non-Mosquito")

    @Test
    fun predictedLabel_usesBundledOrderWhenNoDownloadedClasses() {
        val label = InferenceClassLabels.predictedLabel(
            index = 1,
            downloadedClasses = null,
        ) { bundled.getOrNull(it) }

        assertThat(label).isEqualTo("Culex")
    }

    @Test
    fun predictedLabel_usesDownloadedOrderInsteadOfEnumIndex() {
        val downloaded = listOf("Non-Mosquito", "Anopheles funestus", "Culex")

        val label = InferenceClassLabels.predictedLabel(
            index = 0,
            downloadedClasses = downloaded,
        ) { bundled.getOrNull(it) }

        assertThat(label).isEqualTo("Non-Mosquito")
    }

    @Test
    fun predictedLabel_doesNotFallBackToEnumWhenDownloadedListIsPresent() {
        val label = InferenceClassLabels.predictedLabel(
            index = 5,
            downloadedClasses = listOf("Culex"),
        ) { bundled.getOrNull(it) }

        assertThat(label).isNull()
    }

    @Test
    fun matchesWireValue_ignoresCase() {
        assertThat(InferenceClassLabels.matchesWireValue("non-mosquito", "Non-Mosquito")).isTrue()
        assertThat(InferenceClassLabels.matchesWireValue("Culex", "Non-Mosquito")).isFalse()
        assertThat(InferenceClassLabels.matchesWireValue(null, "Non-Mosquito")).isFalse()
    }
}
