package com.vci.vectorcamapp.intake.domain.strategy

import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.intake.domain.strategy.concrete.FormPresentWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.concrete.ProgramFormAbsentWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.concrete.SurveillanceFormPresentWorkflow
import javax.inject.Inject

class ProgramFormWorkflowFactory @Inject constructor(
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
) {
    suspend fun create(sessionType: SessionType, program: Program): ProgramFormWorkflow {
        return when {
            sessionType == SessionType.DATA_COLLECTION -> ProgramFormAbsentWorkflow()
            program.formVersion != null -> {
                val form = formRepository.getFormByVersion(program.formVersion)
                if (form != null) {
                    val questions = formQuestionRepository.getQuestionsByFormId(form.id)
                    FormPresentWorkflow(form, questions)
                } else {
                    SurveillanceFormPresentWorkflow()
                }
            }
            else -> SurveillanceFormPresentWorkflow()
        }
    }
}
