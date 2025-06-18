package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.data.room.entities.SurveillanceFormEntity

data class SessionWithSiteAndSurveillanceFormRelation(
    @Embedded val session: SessionEntity,

    @Relation(
        parentColumn = "siteId",
        entityColumn = "id"
    )
    val site: SiteEntity,

    @Relation(
        parentColumn = "localId",
        entityColumn = "sessionId"
    )
    val surveillanceForm: SurveillanceFormEntity
)