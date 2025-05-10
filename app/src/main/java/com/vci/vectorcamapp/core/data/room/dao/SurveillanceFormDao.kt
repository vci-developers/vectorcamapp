package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SurveillanceFormEntity

@Dao
interface SurveillanceFormDao {

    @Upsert
    suspend fun upsertSurveillanceForm(surveillanceForm: SurveillanceFormEntity) : Long
}