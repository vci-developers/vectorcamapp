package com.vci.vectorcamapp.core.data.util

import com.vci.vectorcamapp.core.data.dto.site.SiteDto

fun List<SiteDto>.sortByHierarchy(): List<SiteDto> {
    val siteMap = this.associateBy { it.siteId }

    fun calculateDepth(siteId: Int, visited: MutableSet<Int> = mutableSetOf()): Int {
        if (siteId in visited) return 0
        visited.add(siteId)

        val site = siteMap[siteId] ?: return 0
        val parentId = site.parentId

        return if (parentId == null || siteMap[parentId] == null) {
            0
        } else {
            1 + calculateDepth(parentId, visited)
        }
    }

    return this.sortedBy { calculateDepth(it.siteId) }
}
