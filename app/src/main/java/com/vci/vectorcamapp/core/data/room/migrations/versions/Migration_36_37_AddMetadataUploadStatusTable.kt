package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_36_37_ADD_METADATA_UPLOAD_STATUS_TABLE = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `metadata_upload_status` (
                `sessionId` TEXT NOT NULL,
                `step` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`sessionId`, `step`),
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_metadata_upload_status_sessionId` " +
                    "ON `metadata_upload_status` (`sessionId`)"
        )
    }
}
