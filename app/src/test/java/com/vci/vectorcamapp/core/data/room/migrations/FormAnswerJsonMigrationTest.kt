package com.vci.vectorcamapp.core.data.room.migrations

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.converters.FormAnswerValueConverter
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_34_35_STORE_FORM_ANSWER_VALUE_AS_JSON
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FormAnswerJsonMigrationTest {
    @Test
    fun migrationPreservesLegacyStringsAndExistingMultiSelectArrays() {
        val rows = listOf(
            Triple("1", "", "text"),
            Triple("2", " true ", "boolean"),
            Triple("3", "123", "number"),
            Triple("4", "[\"Other\"]", "text"),
            Triple("5", "\"quoted\"\n标本\\", "qr"),
            Triple("6", "[\"A,B\",\"Other\"]", "multi-select"),
            Triple("7", "", "multi-select"),
        )
        val db = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        val statement = mockk<SupportSQLiteStatement>(relaxed = true)
        val migrated = mutableMapOf<String, String>()
        var row = -1
        var id = ""
        var json = ""
        every { db.query(any<String>()) } returns cursor
        every { db.compileStatement(any()) } returns statement
        every { cursor.moveToNext() } answers { ++row < rows.size }
        every { cursor.getString(0) } answers { rows[row].first }
        every { cursor.getString(1) } answers { rows[row].second }
        every { cursor.getString(2) } answers { rows[row].third }
        every { statement.bindString(1, any()) } answers { json = secondArg() }
        every { statement.bindString(2, any()) } answers { id = secondArg() }
        every { statement.executeUpdateDelete() } answers {
            migrated[id] = json
            1
        }

        MIGRATION_34_35_STORE_FORM_ANSWER_VALUE_AS_JSON.migrate(db)

        val converter = FormAnswerValueConverter()
        rows.take(5).forEach { (id, text, _) ->
            assertThat(converter.toFormAnswerValue(migrated.getValue(id)))
                .isEqualTo(FormAnswerValue.StringValue(text))
        }
        assertThat(converter.toFormAnswerValue(migrated.getValue("6")))
            .isEqualTo(FormAnswerValue.ListValue(listOf("A,B", "Other")))
        assertThat(converter.toFormAnswerValue(migrated.getValue("7")))
            .isEqualTo(FormAnswerValue.ListValue(emptyList()))
        verify { db.compileStatement("UPDATE `form_answer` SET `value` = ? WHERE `localId` = ?") }
        verify {
            statement.close()
            cursor.close()
        }
    }
}
