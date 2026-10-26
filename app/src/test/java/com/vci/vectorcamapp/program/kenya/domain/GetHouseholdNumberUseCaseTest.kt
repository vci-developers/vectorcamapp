package com.vci.vectorcamapp.program.kenya.domain
 
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.composites.FormAnswerAndQuestion
import com.vci.vectorcamapp.core.domain.model.composites.FormWithFormAnswersAndQuestions
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID
 
class GetHouseholdNumberUseCaseTest {
    private val repository = mockk<FormRepository>()
    private val lookup = GetHouseholdNumberUseCase(repository)
    private val sessionId = UUID.randomUUID()
 
    private fun answer(key: String?, value: String): FormAnswerAndQuestion = mockk {
        every { question.questionKey } returns key
        every { answer.value } returns FormAnswerValue.StringValue(value)
    }
 
    private fun givenAnswers(vararg answers: FormAnswerAndQuestion) {
        val form = mockk<FormWithFormAnswersAndQuestions>()
        every { form.formAnswersAndQuestions } returns answers.toList()
        coEvery { repository.getFormsWithFormAnswersAndQuestionsBySessionId(sessionId) } returns listOf(form)
    }
 
    @Test
    fun readsOnlyKeyedAnswerFromRequestedSession() = runTest {
        givenAnswers(answer("unrelated", "wrong"), answer(KenyaFormRules.HOUSEHOLD_NUMBER_KEY, " 007 "))
        assertThat(lookup(sessionId)).isEqualTo("007")
        coVerify(exactly = 1) { repository.getFormsWithFormAnswersAndQuestionsBySessionId(sessionId) }
    }
 
    @Test
    fun historicalHouseholdKeyRemainsReadable() = runTest {
        givenAnswers(answer("assign_this_household_a_number", "07"))
        assertThat(lookup(sessionId)).isEqualTo("07")
        givenAnswers(
            answer("household_number", "01"),
            answer("assign_this_household_a_number", "02"),
        )
        assertThat(lookup(sessionId)).isNull()
    }
    @Test
    fun missingBlankOrDuplicateIdentifiersAreNotDisplayed() = runTest {
        givenAnswers(answer(null, "wrong"))
        assertThat(lookup(sessionId)).isNull()
        givenAnswers(answer(KenyaFormRules.HOUSEHOLD_NUMBER_KEY, " "))
        assertThat(lookup(sessionId)).isNull()
        givenAnswers(
            answer(KenyaFormRules.HOUSEHOLD_NUMBER_KEY, "1"),
            answer(KenyaFormRules.HOUSEHOLD_NUMBER_KEY, "2"),
        )
        assertThat(lookup(sessionId)).isNull()
    }
}