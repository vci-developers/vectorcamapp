package com.vci.vectorcamapp.imaging.domain.util

/**
 * Maps an argmax logit index to a label string.
 *
 * Downloaded models carry their own class order. Using the bundled enum ordinal for a model with
 * the same class count and a different order assigns the wrong species, sex, or abdomen status.
 */
object InferenceClassLabels {
    fun predictedLabel(
        index: Int?,
        downloadedClasses: List<String>?,
        bundledWireValueAt: (Int) -> String?,
    ): String? {
        if (index == null || index < 0) return null
        if (!downloadedClasses.isNullOrEmpty()) {
            return downloadedClasses.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
        }
        return bundledWireValueAt(index)
    }

    fun matchesWireValue(predicted: String?, wireValue: String): Boolean {
        return predicted?.equals(wireValue, ignoreCase = true) == true
    }
}
