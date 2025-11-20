package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18_ADD_COLLECTOR_LAST_TRAINED_ON_COLUMN = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `collector` ADD COLUMN `lastTrainedOn` INTEGER NOT NULL DEFAULT 0"
        )

        db.execSQL("ALTER TABLE `session` RENAME TO `session_old`")

        db.execSQL(
            """
            CREATE TABLE `session` (
                `localId` TEXT NOT NULL PRIMARY KEY,
                `siteId` INTEGER NOT NULL,
                `remoteId` INTEGER,
                `collectorTitle` TEXT NOT NULL,
                `collectorName` TEXT NOT NULL,
                `collectorLastTrainedOn` INTEGER NOT NULL DEFAULT 0,
                `collectionDate` INTEGER NOT NULL,
                `collectionMethod` TEXT NOT NULL,
                `specimenCondition` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `submittedAt` INTEGER,
                `notes` TEXT NOT NULL,
                `latitude` REAL,
                `longitude` REAL,
                `type` TEXT NOT NULL,
                FOREIGN KEY(`siteId`) REFERENCES `site`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `session` (
                `localId`, `siteId`, `remoteId`, `collectorTitle`, `collectorName`, 
                `collectorLastTrainedOn`, `collectionDate`, `collectionMethod`, 
                `specimenCondition`, `createdAt`, `completedAt`, `submittedAt`, 
                `notes`, `latitude`, `longitude`, `type`
            )
            SELECT 
                `localId`, `siteId`, `remoteId`, `collectorTitle`, `collectorName`,
                COALESCE(`collectorLastTrainedOn`, 0), `collectionDate`, `collectionMethod`,
                `specimenCondition`, `createdAt`, `completedAt`, `submittedAt`,
                `notes`, `latitude`, `longitude`, `type`
            FROM `session_old`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `session_old`")

        // Recreate indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_siteId` ON `session` (`siteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_completedAt` ON `session` (`completedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_type` ON `session` (`type`)")
    }
}
