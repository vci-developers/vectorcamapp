package com.vci.vectorcamapp.core.presentation.util.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    // region a - Blank / empty query

    @Test
    fun blankQuery_alwaysMatches() {
        assertTrue(SearchUtils.matchesQuery("", listOf("anything")))
    }

    @Test
    fun whitespaceQuery_alwaysMatches() {
        assertTrue(SearchUtils.matchesQuery("   ", listOf("hello")))
    }

    @Test
    fun blankQuery_matchesEmptyFieldList() {
        assertTrue(SearchUtils.matchesQuery("", emptyList()))
    }

    // endregion

    // region b - Simple single-term match

    @Test
    fun termMatchesField_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice", listOf("Alice Smith")))
    }

    @Test
    fun termNotInAnyField_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("bob", listOf("Alice Smith")))
    }

    @Test
    fun caseInsensitiveMatch() {
        assertTrue(SearchUtils.matchesQuery("ALICE", listOf("alice smith")))
    }

    @Test
    fun nullFieldsAreIgnored() {
        assertTrue(SearchUtils.matchesQuery("alice", listOf(null, "Alice Smith")))
    }

    @Test
    fun allNullFields_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice", listOf(null, null)))
    }

    // endregion

    // region c - Word-boundary matching

    @Test
    fun termMatchesAtWordBoundary_returnsTrue() {
        // "alice" starts at a word boundary in "alice smith"
        assertTrue(SearchUtils.matchesQuery("alice", listOf("alice smith")))
    }

    @Test
    fun termMatchesInsideWord_returnsFalse() {
        // "lice" does not start at a word boundary in "alice"
        assertFalse(SearchUtils.matchesQuery("lice", listOf("alice")))
    }

    @Test
    fun termMatchesStartOfSecondWord() {
        assertTrue(SearchUtils.matchesQuery("smith", listOf("alice smith")))
    }

    // endregion

    // region d - Diacritic / accent normalisation

    @Test
    fun queryWithAccent_matchesPlainText() {
        // "café" normalised → "cafe"
        assertTrue(SearchUtils.matchesQuery("café", listOf("cafe")))
    }

    @Test
    fun fieldWithAccent_matchedByPlainQuery() {
        assertTrue(SearchUtils.matchesQuery("cafe", listOf("café")))
    }

    @Test
    fun queryAndFieldBothAccented_match() {
        assertTrue(SearchUtils.matchesQuery("à", listOf("à")))
    }

    // endregion

    // region e - AND semantics (space-separated terms within a group)

    @Test
    fun allTermsPresent_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice smith", listOf("alice smith")))
    }

    @Test
    fun oneTermMissing_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice bob", listOf("alice smith")))
    }

    @Test
    fun termsSpreadAcrossFields_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice smith", listOf("alice", "smith")))
    }

    @Test
    fun extraWhitespaceBetweenTerms_isIgnored() {
        assertTrue(SearchUtils.matchesQuery("alice   smith", listOf("alice smith")))
    }

    // endregion

    // region f - OR semantics (comma-separated groups)

    @Test
    fun firstGroupMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice, bob", listOf("alice")))
    }

    @Test
    fun secondGroupMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice, bob", listOf("bob")))
    }

    @Test
    fun neitherGroupMatches_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice, bob", listOf("charlie")))
    }

    @Test
    fun emptyGroupInOrList_isSkipped() {
        // "alice, , bob" — middle group is blank, treated as false, but alice or bob can still match
        assertTrue(SearchUtils.matchesQuery("alice, , bob", listOf("alice")))
    }

    @Test
    fun commaWithNoGroups_allBlank_returnsFalse() {
        // "," → both groups blank → each blank group evaluates to false → overall false
        assertFalse(SearchUtils.matchesQuery(",", listOf("alice")))
    }

    // endregion

    // region g - Single-string convenience overload

    @Test
    fun singleField_termMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice", "alice in wonderland"))
    }

    @Test
    fun singleField_termMissing_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("bob", "alice in wonderland"))
    }

    // endregion

    // region h - Multiple fields searched together

    @Test
    fun termInSecondField_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("kampala", listOf("Alice Smith", "Kampala District")))
    }

    @Test
    fun termNotInAnyOfMultipleFields_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("xyz", listOf("Alice Smith", "Kampala District")))
    }

    // endregion
}
