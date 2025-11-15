package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_16_17_ADD_SESSION_COLLECTOR_LAST_TRAINED_ON_COLUMN = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `session` ADD COLUMN `collectorLastTrainedOn` INTEGER"
        )
    }
}
