package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.dao.ProgramDao
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgramRepositoryImplementation @Inject constructor(
    private val programDao: ProgramDao
) : ProgramRepository {
    override fun getAllPrograms(): Flow<List<Program>> =
        programDao.getAllPrograms().map { list ->
            list.map { it.toDomain() }
        }
}