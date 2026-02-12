package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    suspend fun upsertProgram(program: Program): Result<Unit, RoomDbError>
    fun observeAllPrograms(): Flow<List<Program>>
    suspend fun getProgramById(programId: Int): Program?
}
