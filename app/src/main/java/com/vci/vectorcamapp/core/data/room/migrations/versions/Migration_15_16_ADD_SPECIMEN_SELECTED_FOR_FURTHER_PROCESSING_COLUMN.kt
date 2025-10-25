package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16_ADD_SPECIMEN_SELECTED_FOR_FURTHER_PROCESSING_COLUMN = object : Migration(15, 16){
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `specimen` ADD COLUMN `shouldProcessFurther` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
