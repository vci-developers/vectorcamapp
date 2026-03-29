package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15_BACKFILL_COLLECTORS = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `collector` (`id`, `name`, `title`)
            SELECT
                LOWER(
                    HEX(RANDOMBLOB(4)) || '-' ||
                    HEX(RANDOMBLOB(2)) || '-' ||
                    '4' || SUBSTR(HEX(RANDOMBLOB(2)), 2) || '-' ||
                    SUBSTR('89ab', ABS(RANDOM()) % 4 + 1, 1) || SUBSTR(HEX(RANDOMBLOB(2)), 2) || '-' ||
                    HEX(RANDOMBLOB(6))
                ) AS `id`,
                base.name,
                base.title
            FROM (
                SELECT DISTINCT
                    TRIM(`collectorName`) AS `name`,
                    TRIM(`collectorTitle`) AS `title`
                FROM `session`
                WHERE `collectorName` IS NOT NULL 
                    AND `collectorTitle` IS NOT NULL
                    AND TRIM(`collectorName`) <> '' 
                    AND TRIM(`collectorTitle`) <> ''
            ) AS base
            """.trimIndent()
        )
    }
}
