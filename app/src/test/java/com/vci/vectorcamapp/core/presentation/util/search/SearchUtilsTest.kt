package com.vci.vectorcamapp.core.presentation.util.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    // region a - Blank / empty query

    @Test
    fun a01_blankQuery_alwaysMatches() {
        assertTrue(SearchUtils.matchesQuery("", listOf("anything")))
    }

    @Test
    fun a02_whitespaceQuery_alwaysMatches() {
        assertTrue(SearchUtils.matchesQuery("   ", listOf("hello")))
    }

    @Test
    fun a03_blankQuery_matchesEmptyFieldList() {
        assertTrue(SearchUtils.matchesQuery("", emptyList()))
    }

    // endregion

    // region b - Simple single-term match

    @Test
    fun b01_termMatchesField_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice", listOf("Alice Smith")))
    }

    @Test
    fun b02_termNotInAnyField_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("bob", listOf("Alice Smith")))
    }

    @Test
    fun b03_caseInsensitiveMatch() {
        assertTrue(SearchUtils.matchesQuery("ALICE", listOf("alice smith")))
    }

    @Test
    fun b04_nullFieldsAreIgnored() {
        assertTrue(SearchUtils.matchesQuery("alice", listOf(null, "Alice Smith")))
    }

    @Test
    fun b05_allNullFields_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice", listOf(null, null)))
    }

    // endregion

    // region c - Word-boundary matching

    @Test
    fun c01_termMatchesAtWordBoundary_returnsTrue() {
        // "alice" starts at a word boundary in "alice smith"
        assertTrue(SearchUtils.matchesQuery("alice", listOf("alice smith")))
    }

    @Test
    fun c02_termMatchesInsideWord_returnsFalse() {
        // "lice" does not start at a word boundary in "alice"
        assertFalse(SearchUtils.matchesQuery("lice", listOf("alice")))
    }

    @Test
    fun c03_termMatchesStartOfSecondWord() {
        assertTrue(SearchUtils.matchesQuery("smith", listOf("alice smith")))
    }

    // endregion

    // region d - Diacritic / accent normalisation

    @Test
    fun d01_queryWithAccent_matchesPlainText() {
        // "café" normalised → "cafe"
        assertTrue(SearchUtils.matchesQuery("café", listOf("cafe")))
    }

    @Test
    fun d02_fieldWithAccent_matchedByPlainQuery() {
        assertTrue(SearchUtils.matchesQuery("cafe", listOf("café")))
    }

    @Test
    fun d03_queryAndFieldBothAccented_match() {
        assertTrue(SearchUtils.matchesQuery("à", listOf("à")))
    }

    // endregion

    // region e - AND semantics (space-separated terms within a group)

    @Test
    fun e01_allTermsPresent_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice smith", listOf("alice smith")))
    }

    @Test
    fun e02_oneTermMissing_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice bob", listOf("alice smith")))
    }

    @Test
    fun e03_termsSpreadAcrossFields_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice smith", listOf("alice", "smith")))
    }

    @Test
    fun e04_extraWhitespaceBetweenTerms_isIgnored() {
        assertTrue(SearchUtils.matchesQuery("alice   smith", listOf("alice smith")))
    }

    // endregion

    // region f - OR semantics (comma-separated groups)

    @Test
    fun f01_firstGroupMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice, bob", listOf("alice")))
    }

    @Test
    fun f02_secondGroupMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice, bob", listOf("bob")))
    }

    @Test
    fun f03_neitherGroupMatches_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("alice, bob", listOf("charlie")))
    }

    @Test
    fun f04_emptyGroupInOrList_isSkipped() {
        // "alice, , bob" — middle group is blank, treated as false, but alice or bob can still match
        assertTrue(SearchUtils.matchesQuery("alice, , bob", listOf("alice")))
    }

    @Test
    fun f05_commaWithNoGroups_allBlank_returnsFalse() {
        // "," → both groups blank → each blank group evaluates to false → overall false
        assertFalse(SearchUtils.matchesQuery(",", listOf("alice")))
    }

    // endregion

    // region g - Single-string convenience overload

    @Test
    fun g01_singleField_termMatches_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("alice", "alice in wonderland"))
    }

    @Test
    fun g02_singleField_termMissing_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("bob", "alice in wonderland"))
    }

    // endregion

    // region h - Multiple fields searched together

    @Test
    fun h01_termInSecondField_returnsTrue() {
        assertTrue(SearchUtils.matchesQuery("kampala", listOf("Alice Smith", "Kampala District")))
    }

    @Test
    fun h02_termNotInAnyField_returnsFalse() {
        assertFalse(SearchUtils.matchesQuery("xyz", listOf("Alice Smith", "Kampala District")))
    }

    // endregion
}
