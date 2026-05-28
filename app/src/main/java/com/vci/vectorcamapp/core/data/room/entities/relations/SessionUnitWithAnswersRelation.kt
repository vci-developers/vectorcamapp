package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity

data class SessionUnitWithAnswersRelation(
    @Embedded val sessionUnitEntity: SessionUnitEntity,
    @Relation(parentColumn = "localId", entityColumn = "sessionUnitId")
    val answerEntities: List<FormAnswerEntity>,
)
