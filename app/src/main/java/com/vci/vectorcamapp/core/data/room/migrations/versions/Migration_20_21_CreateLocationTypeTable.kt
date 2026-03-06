package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_20_21_CREATE_LOCATION_TYPE_TABLE = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `location_type` (
                `id` INTEGER NOT NULL,
                `programId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`programId`) REFERENCES `program`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_location_type_programId` ON `location_type`(`programId`)"
        )
    }
}
