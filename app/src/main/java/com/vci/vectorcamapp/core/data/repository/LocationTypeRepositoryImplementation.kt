package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.LocationTypeDao
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import javax.inject.Inject

class LocationTypeRepositoryImplementation @Inject constructor(
    private val locationTypeDao: LocationTypeDao
): LocationTypeRepository {
    override suspend fun upsertLocationType(locationType: LocationType, programId: Int): Result<Unit, RoomDbError> {
        return try {
            locationTypeDao.upsertLocationType(locationType.toEntity(programId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }
}
