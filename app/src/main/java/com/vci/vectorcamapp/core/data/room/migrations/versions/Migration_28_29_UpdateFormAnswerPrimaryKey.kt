package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_28_29_UPDATE_FORM_ANSWER_PRIMARY_KEY = object: Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `form_answer`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `form_answer` (
                `localId` TEXT NOT NULL,
                `remoteId` INTEGER,
                `sessionId` TEXT NOT NULL,
                `questionId` INTEGER NOT NULL,
                `value` TEXT NOT NULL,
                `dataType` TEXT NOT NULL,
                `submittedAt` INTEGER NOT NULL,
                PRIMARY KEY(`localId`),
                FOREIGN KEY(`questionId`) REFERENCES `form_question`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_questionId` ON `form_answer` (`questionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionId` ON `form_answer` (`sessionId`)")
    }
}
