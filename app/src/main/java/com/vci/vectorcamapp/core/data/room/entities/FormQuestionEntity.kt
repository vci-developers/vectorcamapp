package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "form_question",
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
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("formId"),
        Index("parentId")
    ]
)
data class FormQuestionEntity(
    @PrimaryKey val id: Int = -1,
    val formId: Int = -1,
    val parentId: Int? = null,
    val label: String,
    val type: String,
    val required: Boolean = false,
    val options: List<String>? = null,
    val order: Int? = null
)
