package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_33_34_ADD_SESSION_GPS_ACCURACY_COLUMN = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session` ADD COLUMN `gpsAccuracy` REAL")
    }
}
