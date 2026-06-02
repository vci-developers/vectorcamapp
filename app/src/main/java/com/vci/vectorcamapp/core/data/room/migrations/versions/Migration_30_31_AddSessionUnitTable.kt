package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_30_31_ADD_SESSION_UNIT_TABLE = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_unit` (
                `localId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `remoteId` INTEGER,
                `unitOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`localId`),
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_session_unit_sessionId` " +
                    "ON `session_unit` (`sessionId`)"
        )

        db.execSQL(
            "ALTER TABLE `form_question` ADD COLUMN `answerScope` TEXT NOT NULL " +
                    "DEFAULT 'SESSION'"
        )
        db.execSQL(
            "ALTER TABLE `form_question` ADD COLUMN `isUnitIdentityComponent` INTEGER NOT NULL " +
                    "DEFAULT 0"
        )

        db.execSQL("ALTER TABLE `form_answer` RENAME TO `form_answer_old`")
        db.execSQL(
            """
            CREATE TABLE `form_answer` (
                `localId` TEXT NOT NULL,
                `remoteId` INTEGER,
                `sessionId` TEXT NOT NULL,
                `sessionUnitId` TEXT,
                `questionId` INTEGER NOT NULL,
                `value` TEXT NOT NULL,
                `dataType` TEXT NOT NULL,
                `submittedAt` INTEGER NOT NULL,
                PRIMARY KEY(`localId`),
                FOREIGN KEY(`questionId`) REFERENCES `form_question`(`id`)
                    ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`sessionUnitId`) REFERENCES `session_unit`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `form_answer` (
                `localId`, `remoteId`, `sessionId`, `sessionUnitId`,
                `questionId`, `value`, `dataType`, `submittedAt`
            )
            SELECT
                `localId`, `remoteId`, `sessionId`, NULL,
                `questionId`, `value`, `dataType`, `submittedAt`
            FROM `form_answer_old`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `form_answer_old`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_questionId` ON `form_answer` (`questionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionId` ON `form_answer` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionUnitId` ON `form_answer` (`sessionUnitId`)")

        db.execSQL("ALTER TABLE `specimen` RENAME TO `specimen_old`")
        db.execSQL(
            """
            CREATE TABLE `specimen` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `sessionUnitId` TEXT,
                `remoteId` INTEGER,
                `shouldProcessFurther` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `sessionId`),
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`sessionUnitId`) REFERENCES `session_unit`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `specimen` (
                `id`, `sessionId`, `sessionUnitId`, `remoteId`, `shouldProcessFurther`
            )
            SELECT
                `id`, `sessionId`, NULL, `remoteId`, `shouldProcessFurther`
            FROM `specimen_old`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `specimen_old`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_specimen_sessionId` ON `specimen` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_specimen_sessionUnitId` ON `specimen` (`sessionUnitId`)")
    }
}
