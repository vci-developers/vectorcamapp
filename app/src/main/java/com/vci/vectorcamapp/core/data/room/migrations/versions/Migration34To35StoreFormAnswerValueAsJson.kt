package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vci.vectorcamapp.core.data.room.converters.FormAnswerValueConverter
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue

val MIGRATION_34_35_STORE_FORM_ANSWER_VALUE_AS_JSON = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val converter = FormAnswerValueConverter()
        db.compileStatement("UPDATE `form_answer` SET `value` = ? WHERE `localId` = ?").use { update ->
            db.query("SELECT `localId`, `value`, `dataType` FROM `form_answer`").use { cursor ->
                while (cursor.moveToNext()) {
                    val storedValue = cursor.getString(1)
                    // Scalars were raw text; only multi-select answers were already JSON arrays.
                    val value = legacyFormAnswerValue(storedValue, cursor.getString(2), converter)
                    update.bindString(1, converter.fromFormAnswerValue(value))
                    update.bindString(2, cursor.getString(0))
                    update.executeUpdateDelete()
                }
            }
        }
    }
}
private fun legacyFormAnswerValue(
    storedValue: String,
    dataType: String,
    converter: FormAnswerValueConverter,
): FormAnswerValue {
    if (dataType != "multi-select") return FormAnswerValue.StringValue(storedValue)
    if (storedValue.isBlank()) return FormAnswerValue.ListValue(emptyList())
    val value = converter.toFormAnswerValue(storedValue)
    require(value is FormAnswerValue.ListValue) { "Expected multi-select array" }
    return value
}
