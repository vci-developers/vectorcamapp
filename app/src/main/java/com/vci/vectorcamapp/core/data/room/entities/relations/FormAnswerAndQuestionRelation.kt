package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity

data class FormAnswerAndQuestionRelation(
    @Embedded val answer: FormAnswerEntity,
    @Relation(
        parentColumn = "questionId",
        entityColumn = "id"
    )
    val question: FormQuestionEntity
)
