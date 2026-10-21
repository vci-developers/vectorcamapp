package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_32_33_ADD_SITE_LATITUDE_AND_LONGITUDE_COLUMNS = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `site` ADD COLUMN `latitude` REAL")
        db.execSQL("ALTER TABLE `site` ADD COLUMN `longitude` REAL")
    }
}
