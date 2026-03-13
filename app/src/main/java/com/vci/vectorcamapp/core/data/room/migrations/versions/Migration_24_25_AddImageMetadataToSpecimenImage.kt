package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_24_25_ADD_IMAGE_METADATA_TO_SPECIMEN_IMAGE = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `specimen_image` ADD COLUMN `imageMetadata` TEXT DEFAULT NULL")
    }
}
