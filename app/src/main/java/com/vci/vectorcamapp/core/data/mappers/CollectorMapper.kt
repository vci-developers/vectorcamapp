package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.CollectorEntity
import com.vci.vectorcamapp.core.domain.model.Collector

fun CollectorEntity.toDomain(): Collector {
    return Collector(
        id = this.id,
        name = this.name,
        title = this.title,
        lastTrainedOn = this.lastTrainedOn
    )
}

fun Collector.toEntity(): CollectorEntity {
    return CollectorEntity(
        id = this.id,
        name = this.name,
        title = this.title,
        lastTrainedOn = this.lastTrainedOn
    )
}
