package com.vci.vectorcamapp.intake.domain.strategy.program_form

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.FormPresentWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.IdentityOnlyFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.SurveillanceFormPresentWorkflow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ProgramFormWorkflowFactoryTest {

    private lateinit var formRepository: FormRepository
    private lateinit var formQuestionRepository: FormQuestionRepository
    private lateinit var factory: ProgramFormWorkflowFactory

    private val form = Form(id = 3, name = "Form", version = "v1")

    private val sessionQuestions = listOf(question(10), question(11))
    private val unitQuestions = listOf(
        question(1, identity = true),
        question(2, identity = false),
        question(3, identity = true),
    )

    @Before
    fun setUp() {
        formRepository = mockk(relaxed = true)
        formQuestionRepository = mockk(relaxed = true)
        factory = ProgramFormWorkflowFactory(formRepository, formQuestionRepository)
    }

    private fun stubForm() {
        coEvery { formRepository.getFormByVersion("v1") } returns form
        coEvery {
            formQuestionRepository.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION)
        } returns sessionQuestions
        coEvery {
            formQuestionRepository.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION_UNIT)
        } returns unitQuestions
    }

    @Test
    fun dataCollectionWithForm_exposesOnlyUnitIdentityQuestions() = runTest {
        stubForm()

        val workflow = factory.create(SessionType.DATA_COLLECTION, program("v1"))

        assertThat(workflow).isInstanceOf(IdentityOnlyFormWorkflow::class.java)
        assertThat(workflow.surveillanceForm).isNull()
        assertThat(workflow.form).isNull()
        assertThat(workflow.sessionFormQuestions).isEmpty()
        assertThat(workflow.sessionUnitFormQuestions.map { it.id }).containsExactly(1, 3).inOrder()
    }

    @Test
    fun dataCollectionWithoutForm_exposesNothing() = runTest {
        val workflow = factory.create(SessionType.DATA_COLLECTION, program(null))

        assertThat(workflow).isInstanceOf(IdentityOnlyFormWorkflow::class.java)
        assertThat(workflow.sessionFormQuestions).isEmpty()
        assertThat(workflow.sessionUnitFormQuestions).isEmpty()
    }

    @Test
    fun surveillanceWithForm_exposesEveryQuestionAtBothScopes() = runTest {
        stubForm()

        val workflow = factory.create(SessionType.SURVEILLANCE, program("v1"))

        assertThat(workflow).isInstanceOf(FormPresentWorkflow::class.java)
        assertThat(workflow.form).isEqualTo(form)
        assertThat(workflow.sessionFormQuestions.map { it.id }).containsExactly(10, 11).inOrder()
        assertThat(workflow.sessionUnitFormQuestions.map { it.id })
            .containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun practiceWithForm_mirrorsSurveillance() = runTest {
        stubForm()

        val practice = factory.create(SessionType.PRACTICE, program("v1"))
        val surveillance = factory.create(SessionType.SURVEILLANCE, program("v1"))

        assertThat(practice).isInstanceOf(FormPresentWorkflow::class.java)
        assertThat(practice.form).isEqualTo(surveillance.form)
        assertThat(practice.sessionFormQuestions).isEqualTo(surveillance.sessionFormQuestions)
        assertThat(practice.sessionUnitFormQuestions)
            .isEqualTo(surveillance.sessionUnitFormQuestions)
    }

    @Test
    fun surveillanceWithoutFormVersion_fallsBackToLegacySurveillanceForm() = runTest {
        val workflow = factory.create(SessionType.SURVEILLANCE, program(null))

        assertThat(workflow).isInstanceOf(SurveillanceFormPresentWorkflow::class.java)
        assertThat(workflow.surveillanceForm).isNotNull()
        assertThat(workflow.form).isNull()
        assertThat(workflow.sessionFormQuestions).isEmpty()
        assertThat(workflow.sessionUnitFormQuestions).isEmpty()
    }

    @Test
    fun surveillanceWithUnresolvableForm_fallsBackToLegacySurveillanceForm() = runTest {
        coEvery { formRepository.getFormByVersion("v1") } returns null

        val workflow = factory.create(SessionType.SURVEILLANCE, program("v1"))

        assertThat(workflow).isInstanceOf(SurveillanceFormPresentWorkflow::class.java)
        assertThat(workflow.sessionUnitFormQuestions).isEmpty()
    }

    @Test
    fun practiceWithoutForm_mirrorsSurveillanceFallback() = runTest {
        val workflow = factory.create(SessionType.PRACTICE, program(null))

        assertThat(workflow).isInstanceOf(SurveillanceFormPresentWorkflow::class.java)
        assertThat(workflow.surveillanceForm).isNotNull()
    }

    private fun program(formVersion: String?) = Program(
        id = 7,
        name = "Program",
        country = "UG",
        formVersion = formVersion,
    )

    private fun question(id: Int, identity: Boolean = false) = FormQuestion(
        id = id,
        label = "q$id",
        type = "text",
        required = false,
        prerequisite = null,
        options = null,
        order = id,
        answerScope = FormQuestionScope.SESSION_UNIT,
        isUnitIdentityComponent = identity,
    )
}
