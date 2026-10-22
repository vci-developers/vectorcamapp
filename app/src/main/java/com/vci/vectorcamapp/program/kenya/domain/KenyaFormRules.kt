package com.vci.vectorcamapp.program.kenya.domain
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
/** Program-specific exceptions to the backend-defined form, centralized for this release. */
object KenyaFormRules {
    private const val CONSENT_KEY = "did_the_household_head_or_an_adult_grant_the_con"
    private val numberMaximums = mapOf(
        "how_many_llins_are_in_use_in_this_household" to 30,
        "how_many_conventional_nets_are_in_use_in_this_ho" to 30,
        "how_many_people_slept_in_this_house_last_night" to 30,
        "how_many_people_of_this_house_slept_outdoors_las" to 30,
    )
    private val countBreakdowns = listOf(
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
                    questions.singleOrNull { it.questionKey == key } ?: return@forEach
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
        if (question.type == "number") numberMaximums[question.questionKey] else null
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
        val question = questions.singleOrNull { it.questionKey == CONSENT_KEY } ?: return false
        return question.type == "boolean" &&
            answers[question.id]?.value == FormAnswerValue.StringValue("true")
    }
}