package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_31_32_ADD_PREGNANT_WOMAN_PRESENCE_AND_NUM_CHILDREN_UNDER_5_COLUMNS = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `surveillance_form` ADD COLUMN `numChildrenUnder5` INTEGER")
        db.execSQL("ALTER TABLE `surveillance_form` ADD COLUMN `hasPregnantWoman` INTEGER")

        db.execSQL(
            """
            UPDATE `surveillance_form`
            SET `hasPregnantWoman` = 0
            WHERE `sessionId` IN (
                SELECT `localId` FROM `session` WHERE `completedAt` IS NULL
            )
            """.trimIndent()
        )
    }
}
