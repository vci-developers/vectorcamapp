package com.vci.vectorcamapp.intake.domain.util

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.Site
import org.junit.Test

class SitesShownInIntakeDropdownTest {

    @Test
    fun hidesInactiveSites() {
        val active = site(id = 1, isActive = true, district = "Open")
        val inactive = site(id = 2, isActive = false, district = "Closed")

        assertThat(sitesShownInIntakeDropdown(listOf(inactive, active))).containsExactly(active)
    }

    private fun site(id: Int, isActive: Boolean, district: String) = Site(
        id = id,
        district = district,
        subCounty = null,
        parish = null,
        villageName = "Village",
        houseNumber = "1",
        healthCenter = null,
        isActive = isActive,
        name = null,
        locationHierarchy = null,
        latitude = null,
        longitude = null,
    )
}
