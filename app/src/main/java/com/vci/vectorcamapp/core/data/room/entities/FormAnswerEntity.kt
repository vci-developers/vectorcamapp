package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "form_answer",
    foreignKeys = [
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FormQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("formId"),
        Index("questionId"),
        Index("sessionId")
    ]
)
data class FormAnswerEntity(
    @PrimaryKey val id: Int = -1,
    val sessionId: String,
    val formId: Int = -1,
    val questionId: Int = -1,
    val value: String,
    val dataType: String,
    val submittedAt: Long
)
