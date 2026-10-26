package com.vci.vectorcamapp.program.kenya.domain
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
/** Program-specific exceptions to the backend-defined form, centralized for this release. */
object KenyaFormRules {
    const val HOUSEHOLD_NUMBER_KEY = "household_number"
    // Historical sessions retain their original form definitions and question keys.
    private val legacyKeys = mapOf(
        "assign_this_household_a_number" to HOUSEHOLD_NUMBER_KEY,
        "did_the_household_head_or_an_adult_grant_the_con" to "collection_consent",
        "how_many_llins_are_in_use_in_this_household" to "llins_in_use",
        "how_many_conventional_nets_are_in_use_in_this_ho" to "conventional_nets_in_use",
        "how_many_people_slept_in_this_house_last_night" to "people_slept_indoors",
        "how_many_of_the_people_above_slept_under_llin" to "people_slept_indoors_under_llin",
        "how_many_of_the_people_above_did_not_sleep_under" to "people_slept_indoors_without_llin",
        "how_many_people_of_this_house_slept_outdoors_las" to "people_slept_outdoors",
        "how_many_of_the_people_above_slept_under_llin_2" to "people_slept_outdoors_under_llin",
        "how_many_of_the_people_above_did_not_sleep_under_2" to "people_slept_outdoors_without_llin",
        "where_is_mosquito_collection_taking_place" to "collection_locations",
        "where_is_mosquito_collection_taking_place_2" to "batch_collection_location",
        "scan_cup_id_for_indoors_2" to "collection_cup_id_indoors",
        "scan_cup_id_for_outdoors_2" to "collection_cup_id_outdoors",
        "scan_cup_id_for_animal_shelter_2" to "collection_cup_id_animal_shelter",
        "scan_cup_id_for_indoors" to "batch_cup_id_indoors",
        "scan_cup_id_for_outdoors" to "batch_cup_id_outdoors",
        "scan_cup_id_for_animal_shelter" to "batch_cup_id_animal_shelter",
    )
    private fun canonicalKey(question: FormQuestion): String? =
        legacyKeys[question.questionKey] ?: question.questionKey
    fun householdPrefix(selectedLocations: Collection<String>): String? =
        selectedLocations.mapNotNull { Regex("\\bGP\\s*(\\d{1,2})\\b", RegexOption.IGNORE_CASE).find(it) }
            .map { "GP${it.groupValues[1].padStart(2, '0')}-" }
            .distinct().singleOrNull()
    fun isHouseholdNumber(question: FormQuestion): Boolean = canonicalKey(question) == HOUSEHOLD_NUMBER_KEY
    fun isValidHouseholdNumber(question: FormQuestion, value: FormAnswerValue): Boolean {
        if (!isHouseholdNumber(question)) return true
        val number = (value as? FormAnswerValue.StringValue)?.value ?: return false
        return number.matches(Regex("[0-9]{1,2}")) || (number.isEmpty() && !question.required)
    }
    private const val SITE_RADIUS_METERS = 2000f
    fun isWithinSiteRadius(distanceMeters: Float?): Boolean =
        distanceMeters != null && distanceMeters.isFinite() &&
            distanceMeters >= 0f && distanceMeters <= SITE_RADIUS_METERS
    private const val COLLECTION_LOCATIONS_KEY = "collection_locations"
    private val cupScanPairs = mapOf(
        "batch_cup_id_indoors" to ("collection_cup_id_indoors" to "Indoors"),
        "batch_cup_id_outdoors" to ("collection_cup_id_outdoors" to "Outdoors"),
        "batch_cup_id_animal_shelter" to ("collection_cup_id_animal_shelter" to "Animal Shelter"),
    )
    private val cupLocationTokens = mapOf(
        "Indoors" to "IND",
        "Outdoors" to "OUT",
        "Animal Shelter" to "ANS",
    )
    fun hasValidCupLocation(question: FormQuestion, value: FormAnswerValue): Boolean {
        val token = cupLocationToken(question) ?: return true
        val code = (value as? FormAnswerValue.StringValue)?.value ?: return false
        val parts = code.split('-')
        return question.type == "qr" && parts.size == 3 &&
            parts.all { it.isNotBlank() } && parts[1] == token
    }
    fun cupLocationToken(question: FormQuestion): String? {
        val key = canonicalKey(question)
        val location = cupScanPairs[key]?.second
            ?: cupScanPairs.values.singleOrNull { it.first == key }?.second
            ?: return null
        return cupLocationTokens.getValue(location)
    }
    /** Active scan IDs mapped to their recorded code, or null when no eligible original exists. */
    fun expectedCupCodes(
        sessionQuestions: List<FormQuestion>,
        sessionAnswers: Map<Int, FormAnswer>,
        batchQuestions: List<FormQuestion>,
        batchAnswers: Map<Int, FormAnswer>,
    ): Map<Int, String?> {
        val batchValues = batchAnswers.mapValues { it.value.value }
        val sessionValues = sessionAnswers.mapValues { it.value.value }
        val locationsQuestion = sessionQuestions.singleOrNull {
            canonicalKey(it) == COLLECTION_LOCATIONS_KEY
        }
        val locations = (sessionAnswers[locationsQuestion?.id]?.value as? FormAnswerValue.ListValue)?.value.orEmpty()
        return buildMap {
            batchQuestions.forEach { question ->
                val (originalKey, location) = cupScanPairs[canonicalKey(question)] ?: return@forEach
                if (!FormQuestionPrerequisiteEvaluator.evaluate(question.prerequisite, batchValues)) {
                    return@forEach
                }
                val original = sessionQuestions.singleOrNull { canonicalKey(it) == originalKey }
                val expected = (sessionAnswers[original?.id]?.value as? FormAnswerValue.StringValue)?.value
                val missing = question.type != "qr" || original?.type != "qr" ||
                    locationsQuestion?.type != "multi-select" || location !in locations ||
                    !FormQuestionPrerequisiteEvaluator.evaluate(original.prerequisite, sessionValues) ||
                    expected.isNullOrBlank()
                put(question.id, if (missing) null else expected)
            }
        }
    }
    /** Compare only active batch scans against originals belonging to the current session. */
    fun mismatchedCupQuestionIds(
        sessionQuestions: List<FormQuestion>,
        sessionAnswers: Map<Int, FormAnswer>,
        batchQuestions: List<FormQuestion>,
        batchAnswers: Map<Int, FormAnswer>,
    ): Set<Int> = expectedCupCodes(sessionQuestions, sessionAnswers, batchQuestions, batchAnswers)
        .filter { (id, expected) ->
            expected == null || (batchAnswers[id]?.value as? FormAnswerValue.StringValue)?.value != expected
        }.keys
    private const val CONSENT_KEY = "collection_consent"
    private val numberMaximums = mapOf(
        "llins_in_use" to 30,
        "conventional_nets_in_use" to 30,
        "people_slept_indoors" to 30,
        "people_slept_outdoors" to 30,
    )
    private val countBreakdowns = listOf(
        listOf(
            "people_slept_indoors",
            "people_slept_indoors_under_llin",
            "people_slept_indoors_without_llin",
        ),
        listOf(
            "people_slept_outdoors",
            "people_slept_outdoors_under_llin",
            "people_slept_outdoors_without_llin",
        ),
    )
    /** Return all fields in an incomplete or inconsistent visible breakdown. */
    fun invalidCountBreakdownQuestionIds(
        questions: List<FormQuestion>,
        answers: Map<Int, FormAnswer>,
    ): Set<Int> {
        val values = answers.mapValues { (_, answer) ->
            when (val value = answer.value) {
                is FormAnswerValue.StringValue -> value.copy(value = value.value.trim())
                is FormAnswerValue.ListValue -> value
            }
        }
        return buildSet {
            countBreakdowns.forEach { keys ->
                val fields = keys.map { key ->
                    questions.singleOrNull { canonicalKey(it) == key } ?: return@forEach
                }
                if (fields.any {
                        !FormQuestionPrerequisiteEvaluator.evaluate(it.prerequisite, values)
                    }) return@forEach
                val texts = fields.map { (values[it.id] as? FormAnswerValue.StringValue)?.value }
                if (texts.all { it.isNullOrBlank() }) return@forEach
                val numbers = texts.map { it?.toBigDecimalOrNull() }
                val total = numbers[0]
                val underLlin = numbers[1]
                val notUnderLlin = numbers[2]
                if (fields.any { it.type != "number" } ||
                    total == null || underLlin == null || notUnderLlin == null ||
                    total.compareTo(underLlin + notUnderLlin) != 0) {
                    addAll(fields.map { it.id })
                }
            }
        }
    }
    private fun numberMaximum(question: FormQuestion): Int? =
        if (question.type == "number") numberMaximums[canonicalKey(question)] else null
    // Empty input is allowed while editing; required-field validation handles it on submission.
    fun isWithinNumberLimit(question: FormQuestion, value: FormAnswerValue): Boolean {
        val maximum = numberMaximum(question) ?: return true
        val text = (value as? FormAnswerValue.StringValue)?.value?.trim() ?: return false
        if (text.isEmpty()) return true
        val number = text.toBigDecimalOrNull() ?: return false
        return number <= maximum.toBigDecimal()
    }
    fun hasHouseholdHeadConsent(
        questions: List<FormQuestion>,
        answers: Map<Int, FormAnswer>,
    ): Boolean {
        val question = questions.singleOrNull { canonicalKey(it) == CONSENT_KEY } ?: return false
        return question.type == "boolean" &&
            answers[question.id]?.value == FormAnswerValue.StringValue("true")
    }
}