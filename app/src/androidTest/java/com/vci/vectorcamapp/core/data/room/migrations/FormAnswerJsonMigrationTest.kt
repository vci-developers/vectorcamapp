package com.vci.vectorcamapp.core.data.room.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.converters.FormAnswerValueConverter
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_34_35_STORE_FORM_ANSWER_VALUE_AS_JSON
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormAnswerJsonMigrationTest {
    @Test
    fun upgradesExistingSqliteAnswersWithoutChangingTheirValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "form-answer-json-migration-test"
        val rows = listOf(
            "" to "text",
            "[\"Other\"]" to "text",
            "\"quoted\"\n标本\\" to "qr",
            "true" to "boolean",
            "123" to "number",
            "[\"A,B\",\"Other\"]" to "multi-select",
            "" to "multi-select",
        )
        fun helper(version: Int) = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE form_answer (localId TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL, dataType TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        MIGRATION_34_35_STORE_FORM_ANSWER_VALUE_AS_JSON.migrate(db)
                    }
                }).build()
        )

        context.deleteDatabase(name)
        try {
            helper(34).use { old ->
                rows.forEachIndexed { index, (value, type) ->
                    old.writableDatabase.execSQL(
                        "INSERT INTO form_answer VALUES (?, ?, ?)",
                        arrayOf(index.toString(), value, type),
                    )
                }
            }
            helper(35).use { upgraded ->
                val converter = FormAnswerValueConverter()
                upgraded.writableDatabase.query("SELECT localId, value, dataType FROM form_answer").use { cursor ->
                    var count = 0
                    while (cursor.moveToNext()) {
                        val index = cursor.getString(0).toInt()
                        val expected = when (index) {
                            5 -> FormAnswerValue.ListValue(listOf("A,B", "Other"))
                            6 -> FormAnswerValue.ListValue(emptyList())
                            else -> FormAnswerValue.StringValue(rows[index].first)
                        }
                        assertThat(converter.toFormAnswerValue(cursor.getString(1))).isEqualTo(expected)
                        assertThat(cursor.getString(2)).isEqualTo(rows[index].second)
                        count++
                    }
                    assertThat(count).isEqualTo(rows.size)
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
