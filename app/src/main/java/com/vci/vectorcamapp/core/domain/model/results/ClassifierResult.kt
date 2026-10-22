package com.vci.vectorcamapp.core.domain.model.results

data class ClassifierResult(
    val logits: List<Float>?,
    val inferenceDuration: Long?,
    /** Class order of a downloaded model. Empty means the bundled enum order. */
    val classLabels: List<String> = emptyList(),
)
