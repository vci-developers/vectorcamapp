package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.site.SiteDto
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.domain.model.Site

fun SiteEntity.toDomain() : Site {
    return Site(
        id = this.id,
        district = this.district,
        subCounty = this.subCounty,
        parish = this.parish,
        villageName = this.villageName,
        houseNumber = this.houseNumber,
        healthCenter = this.healthCenter,
        isActive = this.isActive,
        locationTypeId = this.locationTypeId,
        parentId = this.parentId,
        name = this.name,
        locationHierarchy = this.locationHierarchy
    )
}

fun Site.toEntity(programId: Int) : SiteEntity {
    return SiteEntity(
        id = this.id,
        programId = programId,
        district = this.district,
        subCounty = this.subCounty,
        parish = this.parish,
        villageName = this.villageName,
        houseNumber = this.houseNumber,
        healthCenter = this.healthCenter,
        isActive = this.isActive,
        locationTypeId = this.locationTypeId,
        parentId = this.parentId,
        name = this.name,
        locationHierarchy = this.locationHierarchy
    )
}

fun SiteDto.toEntity(): SiteEntity {
    return SiteEntity(
        id = this.id,
        programId = this.programId,
        district = this.district,
        subCounty = this.subCounty,
        parish = this.parish,
        villageName = this.villageName,
        houseNumber = this.houseNumber,
        healthCenter = this.healthCenter,
        isActive = this.isActive,
        locationTypeId = this.locationTypeId,
        parentId = this.parentId,
        name = this.name,
        locationHierarchy = this.locationHierarchy
    )
}
