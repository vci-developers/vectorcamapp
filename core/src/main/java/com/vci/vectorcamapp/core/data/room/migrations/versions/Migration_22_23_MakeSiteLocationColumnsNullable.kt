package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_22_23_MAKE_SITE_LOCATION_COLUMNS_NULLABLE = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `site_new` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `programId` INTEGER NOT NULL,
                `district` TEXT,
                `subCounty` TEXT,
                `parish` TEXT,
                `villageName` TEXT,
                `houseNumber` TEXT,
                `healthCenter` TEXT,
                `isActive` INTEGER NOT NULL,
                `locationTypeId` INTEGER,
                `parentId` INTEGER,
                `name` TEXT,
                `locationHierarchy` TEXT,
                FOREIGN KEY(`programId`) REFERENCES `program`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`locationTypeId`) REFERENCES `location_type`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`parentId`) REFERENCES `site`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `site_new` (
                `id`, `programId`, `district`, `subCounty`, `parish`, `villageName`,
                `houseNumber`, `healthCenter`, `isActive`, `locationTypeId`,
                `parentId`, `name`, `locationHierarchy`
            )
            SELECT 
                `id`, `programId`,
                NULLIF(`district`, ''),
                NULLIF(`subCounty`, ''),
                NULLIF(`parish`, ''),
                NULLIF(`villageName`, ''),
                NULLIF(`houseNumber`, ''),
                NULLIF(`healthCenter`, ''),
                `isActive`, `locationTypeId`, `parentId`, `name`, `locationHierarchy`
            FROM `site`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `site`")
        db.execSQL("ALTER TABLE `site_new` RENAME TO `site`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_programId` ON `site` (`programId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_locationTypeId` ON `site` (`locationTypeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_parentId` ON `site` (`parentId`)")
    }
}
