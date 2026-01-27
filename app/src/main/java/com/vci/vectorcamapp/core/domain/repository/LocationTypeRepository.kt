package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError

interface LocationTypeRepository {
    suspend fun upsertLocationType(locationType: LocationType, programId: Int): Result<Unit, RoomDbError>
}
