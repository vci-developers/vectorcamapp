package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13_REMOVE_SENTINEL_SITE_AND_MOVE_HOUSE_NUMBER_UNDER_SITE =
    object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=ON")

            db.execSQL(
                """
                CREATE TABLE `site_new` (
                    `id` INTEGER NOT NULL,
                    `programId` INTEGER NOT NULL,
                    `district` TEXT NOT NULL DEFAULT '',
                    `subCounty` TEXT NOT NULL DEFAULT '',
                    `parish` TEXT NOT NULL DEFAULT '',
                    `villageName` TEXT NOT NULL DEFAULT '',
                    `houseNumber` TEXT NOT NULL DEFAULT '',
                    `healthCenter` TEXT NOT NULL DEFAULT '',
                    `isActive` INTEGER NOT NULL DEFAULT 1 CHECK (`isActive` IN (0,1)),
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`programId`) REFERENCES `program`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                )
            """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `site_new` (`id`,`programId`,`district`,`subCounty`,`parish`,`villageName`,`houseNumber`,`healthCenter`,`isActive`)
                SELECT `id`,`programId`,`district`,`subCounty`,`parish`,
                     COALESCE(`sentinelSite`, ''),
                     '' AS `houseNumber`,
                     COALESCE(`healthCenter`, ''),
                     1 AS `isActive`
                FROM `site`
            """.trimIndent()
            )

            db.execSQL("DROP TABLE `site`")
            db.execSQL("ALTER TABLE `site_new` RENAME TO `site`")
            db.execSQL("CREATE INDEX `index_site_programId` ON `site` (`programId`)")

            db.execSQL(
                """
                UPDATE `session`
                SET `notes` = CASE
                    WHEN `houseNumber` IS NULL OR TRIM(CAST(`houseNumber` AS TEXT)) = '' THEN COALESCE(`notes`, '')
                    WHEN COALESCE(TRIM(`notes`), '') = '' THEN 'House Number: ' || TRIM(CAST(`houseNumber` AS TEXT))
                    ELSE `notes` || CHAR(10) || 'House Number: ' || TRIM(CAST(`houseNumber` AS TEXT))
                END
                    WHERE `houseNumber` IS NOT NULL
                        AND TRIM(CAST(`houseNumber` AS TEXT)) <> ''
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE `session_new` (
                    `localId` TEXT NOT NULL,
                    `siteId` INTEGER NOT NULL DEFAULT -1,
                    `remoteId` INTEGER,
                    `collectorTitle` TEXT NOT NULL DEFAULT '',
                    `collectorName` TEXT NOT NULL DEFAULT '',
                    `collectionDate` INTEGER NOT NULL DEFAULT 0,
                    `collectionMethod` TEXT NOT NULL DEFAULT '',
                    `specimenCondition` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `completedAt` INTEGER,
                    `submittedAt` INTEGER,
                    `notes` TEXT NOT NULL DEFAULT '',
                    `latitude` REAL,
                    `longitude` REAL,
                    `type` TEXT NOT NULL,
                    PRIMARY KEY(`localId`),
                    FOREIGN KEY(`siteId`) REFERENCES `site`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                  )
            """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `session_new` (
                    `localId`,`siteId`,`remoteId`,
                    `collectorTitle`,`collectorName`,`collectionDate`,
                    `collectionMethod`,`specimenCondition`,
                    `createdAt`,`completedAt`,`submittedAt`,
                    `notes`,`latitude`,`longitude`,`type`
                )
                SELECT
                    `localId`,`siteId`,`remoteId`,
                    COALESCE(`collectorTitle`, ''), COALESCE(`collectorName`, ''), COALESCE(`collectionDate`, 0),
                    COALESCE(`collectionMethod`, ''), COALESCE(`specimenCondition`, ''),
                    COALESCE(`createdAt`, 0), `completedAt`, `submittedAt`,
                    COALESCE(`notes`, ''), `latitude`, `longitude`, `type`
                FROM `session`
            """.trimIndent()
            )

            db.execSQL("DROP TABLE `session`")
            db.execSQL("ALTER TABLE `session_new` RENAME TO `session`")
            db.execSQL("CREATE INDEX `index_session_completedAt` ON `session` (`completedAt`)")
            db.execSQL("CREATE INDEX `index_session_siteId` ON `session` (`siteId`)")
            db.execSQL("CREATE INDEX `index_session_type` ON `session` (`type`)")
        }
    }
