package com.vci.vectorcamapp.complete_session.details.presentation.util

import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import java.text.Normalizer

private fun normalize(text: String): String {
    val lower = text.lowercase().trim()
    val decomp = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return decomp.replace(Regex("\\p{Mn}+"), "")
}

private fun evaluateTerm(needle: String, searchTargetTexts: List<String>): Boolean {
    val pattern = Regex("\\b" + Regex.escape(needle))
    return searchTargetTexts.any { fieldText -> pattern.containsMatchIn(fieldText) }
}

fun matchesQuery(query: String, specimen: Specimen, specimenImage: SpecimenImage): Boolean {
    if (query.isBlank()) return true

    val searchTargetTexts = listOfNotNull(
        specimen.id,
        specimenImage.species,
        specimenImage.sex,
        specimenImage.abdomenStatus
    ).map { normalize(it) }

    val orGroups = query.split(',')

    return orGroups.any { orGroup ->
        val trimmedGroup = orGroup.trim()
        if (trimmedGroup.isBlank()) {
            false
        } else {
            val andTerms = trimmedGroup.split(' ').filter { it.isNotBlank() }

            andTerms.all { term ->
                evaluateTerm(normalize(term), searchTargetTexts)
            }
        }
    }
}
