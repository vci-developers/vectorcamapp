package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3_CREATE_SURVEILLANCE_FORM_TABLE = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS surveillance_form (
                `sessionId` TEXT NOT NULL,
                `country` TEXT NOT NULL,
                `district` TEXT NOT NULL,
                `healthCenter` TEXT NOT NULL,
                `sentinelSite` TEXT NOT NULL,
                `householdNumber` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `collectionDate` INTEGER NOT NULL,
                `collectionMethod` TEXT NOT NULL,
                `collectorName` TEXT NOT NULL,
                `collectorTitle` TEXT NOT NULL,
                `numPeopleSleptInHouse` INTEGER NOT NULL,
                `wasIrsConducted` INTEGER NOT NULL,
                `monthsSinceIrs` INTEGER,
                `numLlinsAvailable` INTEGER NOT NULL,
                `llinType` TEXT,
                `llinBrand` TEXT,
                `numPeopleSleptUnderLlin` INTEGER,
                `notes` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`),
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
            )
            """.trimIndent()
        )
    }
}
