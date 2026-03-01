package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_24_25_ADD_DYNAMIC_FORM_TABLES = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `form` (
                `id` INTEGER NOT NULL,
                `programId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `version` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`programId`) REFERENCES `program`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_programId` ON `form` (`programId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `form_question` (
                `id` INTEGER NOT NULL,
                `formId` INTEGER NOT NULL,
                `parentId` INTEGER,
                `label` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `required` INTEGER NOT NULL,
                `options` TEXT,
                `order` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`formId`) REFERENCES `form`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`parentId`) REFERENCES `form_question`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_question_formId` ON `form_question` (`formId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_question_parentId` ON `form_question` (`parentId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `form_answer` (
                `id` INTEGER NOT NULL,
                `sessionId` TEXT NOT NULL,
                `formId` INTEGER NOT NULL,
                `questionId` INTEGER NOT NULL,
                `value` TEXT NOT NULL,
                `dataType` TEXT NOT NULL,
                `submittedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`formId`) REFERENCES `form`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`questionId`) REFERENCES `form_question`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_formId` ON `form_answer` (`formId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_questionId` ON `form_answer` (`questionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionId` ON `form_answer` (`sessionId`)")
    }
}
