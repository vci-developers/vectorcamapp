package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12_ADD_DETECTION_DURATION_COLUMN = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `inference_result` ADD COLUMN `bboxDetectionDuration` INTEGER DEFAULT 0 NOT NULL")
    }
}
