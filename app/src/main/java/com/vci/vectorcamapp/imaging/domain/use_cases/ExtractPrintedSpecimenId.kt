package com.vci.vectorcamapp.imaging.domain.use_cases
private val printedSpecimenIdPattern = Regex("(?<![A-Za-z0-9])KE[A-Za-z0-9]{4}(?![A-Za-z0-9])")
/** Ignore unrelated label text; do not guess when multiple distinct IDs are visible. */
internal fun extractPrintedSpecimenId(text: String): String? =
    printedSpecimenIdPattern.findAll(text).map { it.value }.distinct().toList().singleOrNull()