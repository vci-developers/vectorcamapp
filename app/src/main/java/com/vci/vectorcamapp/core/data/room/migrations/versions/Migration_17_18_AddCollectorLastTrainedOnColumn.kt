package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18_ADD_COLLECTOR_LAST_TRAINED_ON_COLUMN = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `collector` ADD COLUMN `collectorLastTrainedOn` INTEGER"
        )
    }
}
