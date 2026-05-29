package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity

data class SessionWithFormAnswersRelation(
    @Embedded val sessionEntity: SessionEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "sessionId"
    ) val answerEntities: List<FormAnswerEntity>
)
