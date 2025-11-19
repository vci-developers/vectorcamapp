package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vci.vectorcamapp.core.data.room.entities.SpecimenImageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SpecimenImageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpecimenImage(specimenImage: SpecimenImageEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateSpecimenImage(specimenImage: SpecimenImageEntity): Int

    @Delete
    suspend fun deleteSpecimenImage(specimenImage: SpecimenImageEntity): Int

    @Query("SELECT COUNT(*) FROM specimen_image WHERE sessionId = :sessionId AND metadataUploadStatus = 'COMPLETED'")
    fun observeUploadedMetadataCountForSession(sessionId: UUID): Flow<Int>

    @Query("SELECT COUNT(*) FROM specimen_image WHERE sessionId = :sessionId AND imageUploadStatus = 'COMPLETED'")
    fun observeUploadedImageCountForSession(sessionId: UUID): Flow<Int>

    @Query("SELECT COUNT(*) FROM specimen_image WHERE sessionId = :sessionId")
    suspend fun getTotalCountForSession(sessionId: UUID): Int
}
