package com.vci.vectorcamapp.intake.domain.util

import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Site

fun sitesShownInIntakeDropdown(sites: List<Site>): List<Site> = sites.filter { it.isActive }

object SiteSelectionResolver {

    fun findSelectedSiteWithCoordinates(
        allSites: List<Site>,
        allLocationTypes: List<LocationType>,
        siteSelectionsByLocationTypeId: Map<Int, String>,
        selectedDistrict: String = "",
        selectedVillageName: String = "",
        selectedHouseNumber: String = ""
    ): Site? {
        if (allSites.any { !it.district.isNullOrBlank() }) {
            if (selectedDistrict.isBlank() || selectedVillageName.isBlank() || selectedHouseNumber.isBlank()) {
                return null
            }
            return allSites.filter {
                it.district == selectedDistrict &&
                    it.villageName == selectedVillageName &&
                    it.houseNumber == selectedHouseNumber &&
                    it.latitude != null && it.longitude != null
            }.preferActive()
        }
        val selectedLocationHierarchy = allLocationTypes.mapNotNull { locationType ->
            siteSelectionsByLocationTypeId[locationType.id]?.let { selectedLocationTypeSite ->
                locationType.name to selectedLocationTypeSite
            }
        }

        val selectedSites = selectedLocationHierarchy.indices.mapNotNull { selectedIndex ->
            val selectedLocationHierarchyAtLevel = selectedLocationHierarchy.take(selectedIndex + 1).toMap()
            allSites.filter { site ->
                site.locationHierarchy?.filterValues { it.isNotBlank() } == selectedLocationHierarchyAtLevel
            }.preferActive()
        }

        return selectedSites.lastOrNull { it.latitude != null && it.longitude != null }
    }
}

private fun List<Site>.preferActive(): Site? = firstOrNull { it.isActive } ?: firstOrNull()
