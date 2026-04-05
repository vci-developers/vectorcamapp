package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "form_answer",
    foreignKeys = [
        ForeignKey(
            entity = FormQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("questionId"),
        Index("sessionId")
    ]
)
data class FormAnswerEntity(
    @PrimaryKey val localId: UUID = UUID(0, 0),
    val remoteId: Int? = null,
    val sessionId: UUID = UUID(0, 0),
    val questionId: Int = -1,
    val value: String = "",
    val dataType: String = "",
    val submittedAt: Long = 0L
)
