package com.vci.vectorcamapp.core.data.util

import com.vci.vectorcamapp.core.data.dto.site.SiteDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SortByHierarchyTest {

    private fun site(id: Int, parentId: Int? = null) = SiteDto(siteId = id, parentId = parentId)

    // region a - Single / empty lists

    @Test
    fun a01_emptyList_returnsEmpty() {
        val result = emptyList<SiteDto>().sortByHierarchy()
        assertEquals(emptyList<SiteDto>(), result)
    }

    @Test
    fun a02_singleRoot_returnsSelf() {
        val sites = listOf(site(1))
        val result = sites.sortByHierarchy()
        assertEquals(listOf(site(1)), result)
    }

    // endregion

    // region b - Simple parent-child chains

    @Test
    fun b01_parentBeforeChild_orderedRootFirst() {
        // 1 → root, 2 → child of 1
        val sites = listOf(site(2, parentId = 1), site(1))
        val result = sites.sortByHierarchy()
        assertEquals(listOf(site(1), site(2, parentId = 1)), result)
    }

    @Test
    fun b02_alreadyOrdered_staysInPlace() {
        val sites = listOf(site(1), site(2, parentId = 1))
        val result = sites.sortByHierarchy()
        assertEquals(listOf(site(1), site(2, parentId = 1)), result)
    }

    @Test
    fun b03_threeLevel_chain_sortedRootFirst() {
        // depth: 1=0, 2=1, 3=2
        val sites = listOf(site(3, parentId = 2), site(1), site(2, parentId = 1))
        val result = sites.sortByHierarchy()
        assertEquals(
            listOf(site(1), site(2, parentId = 1), site(3, parentId = 2)),
            result
        )
    }

    // endregion

    // region c - Multiple roots

    @Test
    fun c01_twoRoots_bothAtDepthZero_orderPreservedByStableSort() {
        val site1 = site(1)
        val site2 = site(2)
        val sites = listOf(site1, site2)
        val result = sites.sortByHierarchy()
        // Both have depth 0; sortedBy is stable, so original order preserved
        assertEquals(listOf(site1, site2), result)
    }

    @Test
    fun c02_twoRoots_eachWithChild_rootsBeforeChildren() {
        val sites = listOf(
            site(2, parentId = 1),
            site(4, parentId = 3),
            site(1),
            site(3)
        )
        val result = sites.sortByHierarchy()
        val depths = result.map { it.siteId }
        // Roots (depth 0): 1, 3 before children (depth 1): 2, 4
        val rootIds = setOf(1, 3)
        val childIds = setOf(2, 4)
        val firstRootIdx = depths.indexOfFirst { it in rootIds }
        val lastRootIdx = depths.indexOfLast { it in rootIds }
        val firstChildIdx = depths.indexOfFirst { it in childIds }
        assert(lastRootIdx < firstChildIdx) {
            "All roots should appear before all children, got order: $depths"
        }
    }

    // endregion

    // region d - Node referencing non-existent parent treated as root

    @Test
    fun d01_orphanNode_treatedAsRoot_depth0() {
        // parentId = 999 doesn't exist → treated as root (depth 0)
        val orphan = site(1, parentId = 999)
        val root = site(2)
        val sites = listOf(orphan, root)
        val result = sites.sortByHierarchy()
        // Both at depth 0; stable order preserved
        assertEquals(listOf(orphan, root), result)
    }

    // endregion

    // region e - Cycle detection (visited set prevents infinite recursion)

    @Test
    fun e01_cyclicParentReference_doesNotHang() {
        // site 1 references site 2 as parent, site 2 references site 1 → cycle
        val siteA = site(1, parentId = 2)
        val siteB = site(2, parentId = 1)
        val sites = listOf(siteA, siteB)
        // Must not hang or throw; just verify it completes
        val result = sites.sortByHierarchy()
        assertEquals(2, result.size)
    }

    // endregion
}
