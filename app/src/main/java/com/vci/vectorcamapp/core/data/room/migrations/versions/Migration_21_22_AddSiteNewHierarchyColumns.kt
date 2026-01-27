package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_21_22_UPDATE_SITE_TABLE = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `site` ADD COLUMN `locationTypeId` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `site` ADD COLUMN `parentId` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `site` ADD COLUMN `name` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `site` ADD COLUMN `locationHierarchy` TEXT DEFAULT NULL")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_locationTypeId` ON `site`(`locationTypeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_parentId` ON `site`(`parentId`)")
    }
}
