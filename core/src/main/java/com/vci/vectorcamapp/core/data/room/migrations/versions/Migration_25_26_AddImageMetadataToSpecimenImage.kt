package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26_ADD_IMAGE_METADATA_TO_SPECIMEN_IMAGE = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `specimen_image` ADD COLUMN `imageMetadata` TEXT DEFAULT NULL")
    }
}
