package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_26_27_FIX_FORM_VERSION_TYPE_AND_FORM_ANSWER_SCHEMA = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `program_new` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `country` TEXT NOT NULL,
                `formVersion` TEXT DEFAULT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `program_new` (`id`, `name`, `country`, `formVersion`)
            SELECT `id`, `name`, `country`, CAST(`formVersion` AS TEXT)
            FROM `program`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `program`")
        db.execSQL("ALTER TABLE `program_new` RENAME TO `program`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_country` ON `program` (`country`)")

        db.execSQL("DROP TABLE IF EXISTS `form_answer`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `form_answer` (
                `id` INTEGER NOT NULL,
                `sessionId` TEXT NOT NULL,
                `questionId` INTEGER NOT NULL,
                `value` TEXT NOT NULL,
                `dataType` TEXT NOT NULL,
                `submittedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`questionId`) REFERENCES `form_question`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_questionId` ON `form_answer` (`questionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionId` ON `form_answer` (`sessionId`)")
    }
}