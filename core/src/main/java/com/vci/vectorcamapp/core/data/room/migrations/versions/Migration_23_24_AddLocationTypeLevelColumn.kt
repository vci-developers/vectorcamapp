package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24_ADD_LOCATION_TYPE_LEVEL_COLUMN = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `location_type` ADD COLUMN `level` INTEGER NOT NULL DEFAULT `0`")

        db.execSQL(
            """
                UPDATE `location_type` 
                SET `level` = (
                    SELECT COUNT(*)
                    FROM `location_type` AS lt2
                    WHERE lt2.programId = location_type.programId
                        AND lt2.id <= location_type.id
                )
            """.trimIndent())
    }
}
