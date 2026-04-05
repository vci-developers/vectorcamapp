package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.FormDao
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FormRepositoryImplementation @Inject constructor(
    private val formDao: FormDao
) : FormRepository {

    override suspend fun upsertForm(form: Form, programId: Int): Result<Unit, RoomDbError> {
        return try {
            formDao.upsertForm(form.toEntity(programId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override fun observeFormsByProgramId(programId: Int): Flow<List<Form>> {
        return formDao.observeFormsByProgramId(programId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFormById(id: Int): Form? {
        return formDao.getFormById(id)?.toDomain()
    }

    override suspend fun getFormByVersion(version: String): Form? {
        return formDao.getFormByVersion(version)?.toDomain()
    }
}
