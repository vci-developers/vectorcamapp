package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_29_30_ADD_PREREQUISITE_TO_FORM_QUESTION = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `form_question` ADD COLUMN `prerequisite` TEXT DEFAULT NULL")
    }
}
