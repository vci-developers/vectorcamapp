package com.vci.vectorcamapp.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SpecimenImageDao
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class SpecimenImageRepositoryImplementation @Inject constructor(
    private val specimenImageDao: SpecimenImageDao
): SpecimenImageRepository {
    override suspend fun insertSpecimenImage(specimenImage: SpecimenImage, specimenId: String, sessionId: UUID): Result<Unit, RoomDbError> {
        return try {
            specimenImageDao.insertSpecimenImage(specimenImage.toEntity(specimenId, sessionId))
            Result.Success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.Error(RoomDbError.CONSTRAINT_VIOLATION)
        } catch (e: Exception) {
            Timber.e(e, "SpecimenImageRepository: insertSpecimenImage failed")
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun updateSpecimenImage(specimenImage: SpecimenImage, specimenId: String, sessionId: UUID): Result<Unit, RoomDbError> {
        return try {
            val updatedRows = specimenImageDao.updateSpecimenImage(specimenImage.toEntity(specimenId, sessionId))
            if (updatedRows == 0) {
                Result.Error(RoomDbError.NO_ROWS_AFFECTED)
            } else {
                Result.Success(Unit)
            }
        } catch (e: SQLiteConstraintException) {
            Result.Error(RoomDbError.CONSTRAINT_VIOLATION)
        } catch (e: Exception) {
            Timber.e(e, "SpecimenImageRepository: updateSpecimenImage failed")
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override fun observeUploadedMetadataCountForSession(sessionId: UUID): Flow<Int> {
        return specimenImageDao.observeUploadedMetadataCountForSession(sessionId)
    }

    override fun observeUploadedImageCountForSession(sessionId: UUID): Flow<Int> {
        return specimenImageDao.observeUploadedImageCountForSession(sessionId)
    }

    override suspend fun getTotalCountForSession(sessionId: UUID): Int {
        return specimenImageDao.getTotalCountForSession(sessionId)
    }
    
    override fun observeFailedImageCountForSession(sessionId: UUID): Flow<Int> {
        return specimenImageDao.observeFailedImageCountForSession(sessionId)
    }
}
