package com.vci.vectorcamapp.core.data.dto.site

import kotlinx.serialization.Serializable

@Serializable
data class GetAllSitesResponseDto(
    val sites: List<SiteDto>,
    val total: Int = -1,
    val limit: Int = -1,
    val offset: Int = -1,
    val hasMore: Boolean = false
)
