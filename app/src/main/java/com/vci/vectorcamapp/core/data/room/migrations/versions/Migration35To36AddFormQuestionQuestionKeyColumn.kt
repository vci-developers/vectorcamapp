package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_35_36_ADD_FORM_QUESTION_QUESTION_KEY_COLUMN = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `form_question` ADD COLUMN `questionKey` TEXT")
    }
}
