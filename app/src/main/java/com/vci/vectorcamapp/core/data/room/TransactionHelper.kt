package com.vci.vectorcamapp.core.data.room

import androidx.room.withTransaction

class TransactionHelper(
    private val db: VectorCamDatabase
) {
    suspend fun <T> runAsTransaction(block: suspend () -> T) : T {
        return db.withTransaction(block)
    }
}
