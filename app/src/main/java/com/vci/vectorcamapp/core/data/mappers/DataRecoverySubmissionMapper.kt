package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.DataRecoverySubmissionEntity
import com.vci.vectorcamapp.core.domain.model.DataRecoverySubmission
import java.util.UUID

fun DataRecoverySubmission.toEntity(sessionId: UUID): DataRecoverySubmissionEntity {
    return DataRecoverySubmissionEntity(
        idempotencyKey = this.idempotencyKey,
        sessionId = sessionId,
        step = this.step,
        dataRecoveryId = this.dataRecoveryId,
        submittedAt = this.submittedAt,
    )
}
