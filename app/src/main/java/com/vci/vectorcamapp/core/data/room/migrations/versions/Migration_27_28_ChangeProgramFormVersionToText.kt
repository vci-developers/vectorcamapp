package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_27_28_CHANGE_PROGRAM_FORM_VERSION_TO_TEXT = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")

        db.execSQL("ALTER TABLE `program` RENAME TO `program_old`")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `program` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `country` TEXT NOT NULL,
                `formVersion` TEXT DEFAULT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `program` (`id`, `name`, `country`, `formVersion`)
            SELECT `id`, `name`, `country`, CAST(`formVersion` AS TEXT)
            FROM `program_old`
        """.trimIndent())
        db.execSQL("DROP TABLE `program_old`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_country` ON `program` (`country`)")

        db.execSQL("PRAGMA foreign_keys=ON")
    }
}