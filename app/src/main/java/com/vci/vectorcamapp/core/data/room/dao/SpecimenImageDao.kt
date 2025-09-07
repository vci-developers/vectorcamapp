package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vci.vectorcamapp.complete_session.list.domain.model.CompleteSessionListUploadCount
import com.vci.vectorcamapp.core.data.room.entities.SpecimenImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecimenImageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpecimenImage(specimenImage: SpecimenImageEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateSpecimenImage(specimenImage: SpecimenImageEntity): Int

    @Delete
    suspend fun deleteSpecimenImage(specimenImage: SpecimenImageEntity): Int

    @Query("""
        SELECT 
            si.sessionId as sessionId,
            COUNT(CASE WHEN si.metadataUploadStatus = 'COMPLETED' 
                       AND si.imageUploadStatus = 'COMPLETED' 
                  THEN 1 END) as uploadedImages,
            COUNT(si.localId) as totalImages
        FROM specimen_image si
        WHERE si.sessionId IN (
            SELECT s.localId FROM session s WHERE s.completedAt IS NOT NULL
        )
        GROUP BY si.sessionId
    """)
    fun observeSessionUploadCounts(): Flow<List<CompleteSessionListUploadCount>>
}
