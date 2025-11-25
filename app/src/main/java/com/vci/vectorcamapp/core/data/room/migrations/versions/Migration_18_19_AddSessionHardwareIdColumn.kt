package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_18_19_ADD_SESSION_HARDWARE_ID_COLUMN = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session` ADD COLUMN `hardwareId` TEXT NOT NULL DEFAULT 'XXXXXX'")
    }
}
