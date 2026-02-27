package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_24_25_ADD_EXPECTED_SPECIMENS_AND_EXPECTED_IMAGES_COLUMNS = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `session` ADD COLUMN `expectedSpecimens` INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE `specimen` ADD COLUMN `expectedImages` INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                """
                UPDATE `session`
                SET `expectedSpecimens` = (
                    SELECT COUNT(*) FROM `specimen` WHERE `specimen`.`sessionId` = `session`.`localId`
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE `specimen`
                SET `expectedImages` = (
                    SELECT COUNT(*) FROM `specimen_image`
                    WHERE `specimen_image`.`specimenId` = `specimen`.`id`
                    AND `specimen_image`.`sessionId` = `specimen`.`sessionId`
                )
                """.trimIndent()
            )
        }
    }
