package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_30_31_ADD_COLLECTION_CYCLE_TO_SESSION = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session` ADD COLUMN `collectionCycle` TEXT DEFAULT NULL")
    }
}
