package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2_CREATE_BOUNDING_BOX_TABLE = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bounding_box` (
                `specimenId` TEXT NOT NULL,
                `topLeftX` REAL NOT NULL,
                `topLeftY` REAL NOT NULL,
                `width` REAL NOT NULL,
                `height` REAL NOT NULL,
                `confidence` REAL NOT NULL,
                `classId` INTEGER NOT NULL,
                PRIMARY KEY(`specimenId`),
                FOREIGN KEY(`specimenId`) REFERENCES `specimen`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """.trimIndent()
        )
    }

}