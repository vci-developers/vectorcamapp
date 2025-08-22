package com.vci.vectorcamapp.complete_session.details.presentation.util

import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import java.text.Normalizer

private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")

private fun normalize(text: String): String {
    val lower = text.lowercase().trim()
    val decomp = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return decomp.replace(Regex("\\p{Mn}+"), "")
}

private fun tokensOf(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return TOKEN_REGEX.findAll(normalize(text)).map { it.value }.toList()
}

fun matchesQuery(query: String, specimen: Specimen, specimenImage: SpecimenImage): Boolean {
    val needles = tokensOf(query)
    if (needles.isEmpty()) return true

    val haystack = buildList {
        addAll(tokensOf(specimen.id))
        addAll(tokensOf(specimenImage.species))
        addAll(tokensOf(specimenImage.sex))
        addAll(tokensOf(specimenImage.abdomenStatus))
    }

    return needles.all { needle -> haystack.any { token -> token.startsWith(needle) } }
}
