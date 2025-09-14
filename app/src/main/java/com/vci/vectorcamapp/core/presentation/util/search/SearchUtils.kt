package com.vci.vectorcamapp.core.presentation.util.search

import java.text.Normalizer

object SearchUtils {
    private fun normalize(text: String): String {
        val lower = text.lowercase().trim()
        val decomposedText = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return decomposedText.replace(Regex("\\p{Mn}+"), "")
    }

    private fun evaluateTerm(needle: String, searchTargetTexts: List<String>): Boolean {
        val pattern = Regex("\\b" + Regex.escape(needle))
        return searchTargetTexts.any { fieldText -> pattern.containsMatchIn(fieldText) }
    }

    fun matchesQuery(query: String, fields: List<String?>): Boolean {
        if (query.isBlank()) return true

        val searchTargetTexts = fields.filterNotNull().map { normalize(it) }
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

    fun matchesQuery(query: String, text: String): Boolean =
        matchesQuery(query, listOf(text))
}