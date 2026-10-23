package com.vci.vectorcamapp.program.kenya.domain
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateFormAnswersUseCase
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import org.junit.Test
import java.util.UUID
import com.vci.vectorcamapp.collection_batch.domain.use_cases.ValidateFormAnswersUseCase as ValidateBatchAnswers
class KenyaFormRulesTest {
    @Test
    fun expectedCupCodesOnlyIncludeSelectedLocationAndDistinguishMissingOriginal() {
        val selection = question("where_is_mosquito_collection_taking_place_2").copy(
            id = 10, type = "select"
        )
        val scan = question("scan_cup_id_for_indoors").copy(
            id = 11, type = "qr",
            prerequisite = FormQuestionPrerequisiteExpression.Predicate(
                10, "eq", FormQuestionPrerequisiteValue.StringValue("Indoors")
            )
        )
        fun expected(location: String) = KenyaFormRules.expectedCupCodes(
            emptyList(), emptyMap(), listOf(selection, scan),
            mapOf(10 to answers(location).getValue(1))
        )
        assertThat(expected("")).isEmpty()
        assertThat(expected("Indoors")).containsExactly(11, null)
        assertThat(expected("Outdoors")).isEmpty()
    }
    @Test
    fun cupLocationTokensAreCheckedForBothOriginalsAndRescans() {
        listOf("indoors" to "IN", "outdoors" to "OUT", "animal_shelter" to "ANS")
            .forEach { (suffix, token) ->
                listOf("scan_cup_id_for_$suffix", "scan_cup_id_for_${suffix}_2")
                    .forEach { key ->
                        val field = question(key).copy(id = 1, type = "qr")
                        val valid = answers("GP01-$token-CUP01")
                        assertThat(KenyaFormRules.hasValidCupLocation(field, valid.getValue(1).value)).isTrue()
                        assertThat(ValidateFormAnswersUseCase()(listOf(field), valid)[1])
                            .isInstanceOf(Result.Success::class.java)
                        assertThat(ValidateBatchAnswers()(listOf(field), valid)[1])
                            .isInstanceOf(Result.Success::class.java)
                        listOf("", "GP01-X${token}-CUP01", "GP01-${token}X-CUP01",
                            "GP01-${token.lowercase()}-CUP01", "GP01-$token-CUP01-EXTRA",
                            "GP01-CUP01-$token", "-$token-CUP01", "GP01-$token-")
                            .forEach { code ->
                                val invalid = answers(code)
                                assertThat(KenyaFormRules.hasValidCupLocation(field, invalid.getValue(1).value))
                                    .isFalse()
                                assertThat(ValidateFormAnswersUseCase()(listOf(field), invalid)[1])
                                    .isEqualTo(Result.Error(FormValidationError.INVALID_CUP_LOCATION))
                                assertThat(ValidateBatchAnswers()(listOf(field), invalid)[1])
                                    .isEqualTo(Result.Error(
                                        CollectionBatchFormError.INVALID_CUP_LOCATION
                                    ))
                            }
                    }
            }
        assertThat(KenyaFormRules.hasValidCupLocation(question("unrelated"), value("anything"))).isTrue()
    }
    @Test
    fun siteRadiusIsInclusiveAndRejectsUnverifiableDistances() {
        listOf(0f, 1999f, 2000f).forEach {
            assertThat(KenyaFormRules.isWithinSiteRadius(it)).isTrue()
        }
        listOf(null, -1f, 2000.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertThat(KenyaFormRules.isWithinSiteRadius(it)).isFalse()
        }
    }
    @Test
    fun cupRescansMatchOnlyTheirSelectedLocationAndCurrentSessionOriginal() {
        listOf("indoors" to "Indoors", "outdoors" to "Outdoors", "animal_shelter" to "Animal Shelter")
            .forEach { (suffix, location) ->
                val selection = question("where_is_mosquito_collection_taking_place").copy(id = 1, type = "multi-select")
                val original = question("scan_cup_id_for_${suffix}_2").copy(id = 2, type = "qr")
                val rescan = question("scan_cup_id_for_$suffix").copy(id = 3, type = "qr",
                    answerScope = FormQuestionScope.SESSION_UNIT)
                val sessionAnswers = mapOf(
                    1 to answers("")[1]!!.copy(value = FormAnswerValue.ListValue(listOf(location))),
                    2 to answers("GP01-IN-CUP01")[1]!!,
                )
                fun validate(actual: String, saved: Map<Int, FormAnswer> = sessionAnswers) =
                    KenyaFormRules.mismatchedCupQuestionIds(
                        listOf(selection, original), saved, listOf(rescan),
                        mapOf(3 to answers(actual)[1]!!)
                    )
                assertThat(validate("GP01-IN-CUP01")).isEmpty()
                listOf("", "GP01-IN-CUP02", "gp01-in-cup01", " GP01-IN-CUP01").forEach {
                    assertThat(validate(it)).containsExactly(3)
                }
                assertThat(validate("GP01-IN-CUP01", sessionAnswers - 2)).containsExactly(3)
                assertThat(validate("GP01-IN-CUP01", sessionAnswers - 1)).containsExactly(3)
                assertThat(validate("GP01-IN-CUP01", emptyMap())).containsExactly(3)
                val hidden = rescan.copy(prerequisite = FormQuestionPrerequisiteExpression.Predicate(
                    99, "eq", FormQuestionPrerequisiteValue.StringValue("Outdoors")
                ))
                assertThat(KenyaFormRules.mismatchedCupQuestionIds(
                    listOf(selection, original), sessionAnswers, listOf(hidden), emptyMap()
                )).isEmpty()
            }
    }
    private val breakdownKeys = listOf(
        listOf(
            "how_many_people_slept_in_this_house_last_night",
            "how_many_of_the_people_above_slept_under_llin",
            "how_many_of_the_people_above_did_not_sleep_under",
        ),
        listOf(
            "how_many_people_of_this_house_slept_outdoors_las",
            "how_many_of_the_people_above_slept_under_llin_2",
            "how_many_of_the_people_above_did_not_sleep_under_2",
        ),
    )
    private fun answers(vararg texts: String) = texts.mapIndexed { index, text ->
        index + 1 to FormAnswer(UUID.randomUUID(), null, value(text), "number", 0L)
    }.toMap()
    @Test
    fun bothEquationsValidateTotalsAndReportAllThreeFields() {
        breakdownKeys.forEach { keys ->
            val questions = keys.mapIndexed { index, key -> question(key).copy(id = index + 1) }
            listOf(
                listOf("5", "2", "3"),
                listOf("0", "0", "0"),
                listOf("5.00", "2", "3.0"),
                listOf("0.3", "0.1", "0.2"),
            ).forEach { texts ->
                val results = ValidateFormAnswersUseCase()(questions, answers(*texts.toTypedArray()))
                assertThat(results.values.all { it is Result.Success }).isTrue()
            }
            listOf(
                listOf("6", "2", "3"),
                listOf("4", "2", "3"),
                listOf("5", "", "3"),
                listOf("", "2", "3"),
                listOf("5", "NaN", "3"),
            ).forEach { texts ->
                val results = ValidateFormAnswersUseCase()(questions, answers(*texts.toTypedArray()))
                results.values.forEach {
                    assertThat(it).isEqualTo(Result.Error(FormValidationError.INCONSISTENT_PEOPLE_COUNTS))
                }
            }
        }
    }
    @Test
    fun missingQuestionsAndHiddenBreakdownsDoNotBlockUnrelatedForms() {
        val questions = breakdownKeys.first().mapIndexed { index, key -> question(key).copy(id = index + 1) }
        assertThat(KenyaFormRules.invalidCountBreakdownQuestionIds(questions.take(2), answers("5", "2")))
            .isEmpty()
        assertThat(KenyaFormRules.invalidCountBreakdownQuestionIds(questions, emptyMap())).isEmpty()
        val conditional = questions.map {
            it.copy(prerequisite = FormQuestionPrerequisiteExpression.Predicate(
                questionId = 99, operator = "eq",
                value = FormQuestionPrerequisiteValue.StringValue("true")
            ))
        }
        assertThat(KenyaFormRules.invalidCountBreakdownQuestionIds(conditional, answers("5", "1", "1")))
            .isEmpty()
    }
    @Test
    fun indoorAndOutdoorEquationsAreIndependent() {
        val questions = breakdownKeys.flatten().mapIndexed { index, key -> question(key).copy(id = index + 1) }
        assertThat(KenyaFormRules.invalidCountBreakdownQuestionIds(
            questions, answers("5", "2", "3", "4", "1", "1")
        )).containsExactly(4, 5, 6)
    }
    private val keys = listOf(
        "how_many_llins_are_in_use_in_this_household",
        "how_many_conventional_nets_are_in_use_in_this_ho",
        "how_many_people_slept_in_this_house_last_night",
        "how_many_people_of_this_house_slept_outdoors_las",
    )
    private fun question(key: String?) = FormQuestion(
        42, "Count", key, "number", false, null, null, 1, FormQuestionScope.SESSION, false
    )
    private fun value(text: String) = FormAnswerValue.StringValue(text)
    @Test
    fun allFourKeysHaveInclusiveMaximumAndAllowClearing() {
        keys.forEach { key ->
            val question = question(key)
            listOf("", "0", "29", "30", "30.", "30.00", "00030").forEach {
                assertThat(KenyaFormRules.isWithinNumberLimit(question, value(it))).isTrue()
            }
            listOf("30.01", "31", "999999999999999999999999999999", "NaN", "Infinity").forEach {
                assertThat(KenyaFormRules.isWithinNumberLimit(question, value(it))).isFalse()
            }
        }
    }
    @Test
    fun unrelatedQuestionsKeepExistingBehavior() {
        listOf(null, "another_question").forEach {
            assertThat(KenyaFormRules.isWithinNumberLimit(question(it), value("999"))).isTrue()
        }
        assertThat(KenyaFormRules.isWithinNumberLimit(question(keys.first()).copy(type = "text"), value("999")))
            .isTrue()
    }
    @Test
    fun intakeAndBatchValidationRejectStoredValuesAboveMaximum() {
        keys.forEach { key ->
            val question = question(key)
            listOf("30" to true, "31" to false, "30.01" to false).forEach { (text, valid) ->
                val answers = mapOf(42 to FormAnswer(UUID.randomUUID(), null, value(text), "number", 0L))
                assertThat(ValidateFormAnswersUseCase()(listOf(question), answers)[42] is Result.Success)
                    .isEqualTo(valid)
                assertThat(ValidateBatchAnswers()(listOf(question), answers)[42] is Result.Success)
                    .isEqualTo(valid)
            }
        }
    }
}