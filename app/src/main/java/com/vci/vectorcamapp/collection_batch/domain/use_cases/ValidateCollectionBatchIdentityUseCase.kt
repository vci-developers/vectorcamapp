package com.vci.vectorcamapp.collection_batch.domain.use_cases

import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

class ValidateCollectionBatchIdentityUseCase @Inject constructor() {
    operator fun invoke(
        formQuestions: List<FormQuestion>,
        draftAnswersByQuestionId: Map<Int, FormAnswer>,
        existingAnswersBySessionUnitId: Map<UUID, Map<Int, FormAnswer>>,
        editingSessionUnitId: UUID?
    ): Result<Unit, CollectionBatchFormError> {
        val identityQuestionIds =
            formQuestions.filter { it.answerScope == FormQuestionScope.SESSION_UNIT && it.isUnitIdentityComponent }
                .map { it.id }
        if (identityQuestionIds.isEmpty()) return Result.Success(Unit)

        val draftIdentity = identityQuestionIds.associateWith { questionId ->
            draftAnswersByQuestionId[questionId]?.value.orEmpty().trim()
        }
        if (draftIdentity.values.any { it.isBlank() }) return Result.Success(Unit)

        val isDuplicate = existingAnswersBySessionUnitId.filterKeys { it != editingSessionUnitId }
            .any { (_, existingAnswers) ->
                identityQuestionIds.all { questionId ->
                    existingAnswers[questionId]?.value.orEmpty().trim() == draftIdentity.getValue(
                        questionId
                    )
                }
            }

        return if (isDuplicate) {
            Result.Error(CollectionBatchFormError.DUPLICATE_IDENTITY)
        } else {
            Result.Success(Unit)
        }
    }
}