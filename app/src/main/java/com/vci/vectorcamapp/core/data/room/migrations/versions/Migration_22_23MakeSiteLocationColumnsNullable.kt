package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_22_23_MAKE_SITE_LOCATION_COLUMNS_NULLABLE = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE SiteEntity_new (
                id INTEGER NOT NULL PRIMARY KEY,
                programId INTEGER NOT NULL,
                district TEXT,
                subCounty TEXT,
                parish TEXT,
                villageName TEXT,
                houseNumber TEXT,
                healthCenter TEXT,
                isActive INTEGER NOT NULL,
                locationTypeId INTEGER,
                parentId INTEGER,
                name TEXT,
                locationHierarchy TEXT
            )
            """.trimIndent())

        db.execSQL(
            """
            INSERT INTO SiteEntity_new (
                id, programId, district, subCounty, parish, villageName,
                houseNumber, healthCenter, isActive, locationTypeId,
                parentId, name, locationHierarchy
            )
            SELECT 
                id, programId,
                NULLIF(district, ''),
                NULLIF(subCounty, ''),
                NULLIF(parish, ''),
                NULLIF(villageName, ''),
                NULLIF(houseNumber, ''),
                NULLIF(healthCenter, ''),
                isActive, locationTypeId, parentId, name, locationHierarchy
            FROM SiteEntity
            """.trimIndent())

        db.execSQL("DROP TABLE SiteEntity")
        db.execSQL("ALTER TABLE SiteEntity_new RENAME TO SiteEntity")
    }
}
