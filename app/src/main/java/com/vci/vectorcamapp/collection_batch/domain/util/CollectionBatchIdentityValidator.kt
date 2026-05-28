package com.vci.vectorcamapp.collection_batch.domain.util

import com.vci.vectorcamapp.core.domain.model.FormQuestion
import java.util.UUID

object CollectionBatchIdentityValidator {

    /**
     * Returns true if the proposed (draft) bucket name for [editingUnitId] would collide with any
     * existing unit's bucket name in [existingUnits]. Used to block duplicate identity within a
     * single session.
     *
     * @param existingUnits map of unitLocalId → that unit's answers keyed by questionId
     * @param editingUnitId pass the localId of the unit being edited, or null when creating new.
     */
    fun wouldDuplicate(
        questions: List<FormQuestion>,
        draftAnswers: Map<Int, String>,
        existingUnits: Map<UUID, Map<Int, String>>,
        editingUnitId: UUID?,
    ): Boolean {
        val draft = CollectionBatchIdentityResolver.deriveBucketName(questions, draftAnswers)
        if (draft.isBlank()) return false

        return existingUnits.any { (unitId, answers) ->
            unitId != editingUnitId &&
                CollectionBatchIdentityResolver.deriveBucketName(questions, answers) == draft
        }
    }
}
