package com.vci.vectorcamapp.core.data.dto.site

import kotlinx.serialization.Serializable

@Serializable
data class GetAllSitesResponseDto(
    val sites: List<SiteDto>
)
