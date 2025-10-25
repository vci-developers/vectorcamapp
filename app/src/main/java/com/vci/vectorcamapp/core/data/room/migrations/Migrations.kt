package com.vci.vectorcamapp.core.data.room.migrations

import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_10_11_ADD_CLASSIFICATION_DURATION_COLUMNS
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_11_12_ADD_DETECTION_DURATION_COLUMN
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_12_13_REMOVE_SENTINEL_SITE_AND_MOVE_HOUSE_NUMBER_UNDER_SITE
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_13_14_ADD_COLLECTOR_TABLE
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_14_15_BACKFILL_COLLECTORS
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_1_2_CREATE_BOUNDING_BOX_TABLE
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_2_3_CREATE_SURVEILLANCE_FORM_TABLE
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_3_4_SCHEMA_V2
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_4_5_ADD_SUBMITTED_AT_COLUMNS_TO_SPECIMEN_AND_SURVEILLANCE_FORM_TABLES
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_5_6_ADD_UPLOAD_STATUS_TO_SPECIMEN
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_6_7_BOUNDING_BOX_TO_INFERENCE_RESULT
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_7_8_SPECIMEN_IMAGE_SEPARATION
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_8_9_ADD_SESSION_TYPE_COLUMN
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_9_10_REFACTOR_SPECIMEN_IMAGE_AND_INFERENCE_RESULT_KEYS
import com.vci.vectorcamapp.core.data.room.migrations.versions.Migration_15_16_ADD_SPECIMEN_SELECTED_FOR_FURTHER_PROCESSING_COLUMN

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2_CREATE_BOUNDING_BOX_TABLE,
    MIGRATION_2_3_CREATE_SURVEILLANCE_FORM_TABLE,
    MIGRATION_3_4_SCHEMA_V2,
    MIGRATION_4_5_ADD_SUBMITTED_AT_COLUMNS_TO_SPECIMEN_AND_SURVEILLANCE_FORM_TABLES,
    MIGRATION_5_6_ADD_UPLOAD_STATUS_TO_SPECIMEN,
    MIGRATION_6_7_BOUNDING_BOX_TO_INFERENCE_RESULT,
    MIGRATION_7_8_SPECIMEN_IMAGE_SEPARATION,
    MIGRATION_8_9_ADD_SESSION_TYPE_COLUMN,
    MIGRATION_9_10_REFACTOR_SPECIMEN_IMAGE_AND_INFERENCE_RESULT_KEYS,
    MIGRATION_10_11_ADD_CLASSIFICATION_DURATION_COLUMNS,
    MIGRATION_11_12_ADD_DETECTION_DURATION_COLUMN,
    MIGRATION_12_13_REMOVE_SENTINEL_SITE_AND_MOVE_HOUSE_NUMBER_UNDER_SITE,
    MIGRATION_13_14_ADD_COLLECTOR_TABLE,
    MIGRATION_14_15_BACKFILL_COLLECTORS,
    Migration_15_16_ADD_SPECIMEN_SELECTED_FOR_FURTHER_PROCESSING_COLUMN,
)
