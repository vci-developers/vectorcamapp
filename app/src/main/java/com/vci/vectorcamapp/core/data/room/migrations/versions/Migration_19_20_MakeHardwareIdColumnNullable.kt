package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20_MAKE_HARDWARE_ID_COLUMN_NULLABLE = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session` RENAME TO `session_old`")

        db.execSQL(
            """
            CREATE TABLE `session` (
                `localId` TEXT NOT NULL PRIMARY KEY,
                `siteId` INTEGER NOT NULL,
                `remoteId` INTEGER,
                `collectorTitle` TEXT NOT NULL,
                `collectorName` TEXT NOT NULL,
                `collectorLastTrainedOn` INTEGER NOT NULL,
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
                `hardwareId` TEXT,
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
                `notes`, `latitude`, `longitude`, `type`, `hardwareId`
            )
            SELECT 
                `localId`, `siteId`, `remoteId`, `collectorTitle`, `collectorName`,
                `collectorLastTrainedOn`, `collectionDate`, `collectionMethod`,
                `specimenCondition`, `createdAt`, `completedAt`, `submittedAt`,
                `notes`, `latitude`, `longitude`, `type`, `hardwareId`
            FROM `session_old`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `session_old`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_siteId` ON `session` (`siteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_completedAt` ON `session` (`completedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_type` ON `session` (`type`)")
    }
}
