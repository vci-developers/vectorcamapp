package com.vci.vectorcamapp.core.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vci.vectorcamapp.core.data.room.converters.UriConverter
import com.vci.vectorcamapp.core.data.room.converters.UuidConverter
import com.vci.vectorcamapp.core.data.room.dao.SessionDao
import com.vci.vectorcamapp.core.data.room.dao.SpecimenDao
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity

@Database(
    entities = [SessionEntity::class, SpecimenEntity::class],
    version = 1,
)
@TypeConverters(UuidConverter::class, UriConverter::class)
abstract class VectorCamDatabase : RoomDatabase() {
    abstract val sessionDao: SessionDao
    abstract val specimenDao: SpecimenDao
}
