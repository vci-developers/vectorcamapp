package com.vci.vectorcamapp.core.data.room.migrations

import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_1_2_CREATE_BOUNDING_BOX_TABLE
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_2_3_CREATE_SURVEILLANCE_FORM_TABLE

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2_CREATE_BOUNDING_BOX_TABLE,
    MIGRATION_2_3_CREATE_SURVEILLANCE_FORM_TABLE
)