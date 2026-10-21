package com.vci.vectorcamapp.intake.domain.util

import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Site

object SiteSelectionResolver {

    fun findSelectedSiteWithCoordinates(
        allSites: List<Site>,
        allLocationTypes: List<LocationType>,
        siteSelectionsByLocationTypeId: Map<Int, String>
    ): Site? {
        val selectedLocationHierarchy = allLocationTypes.mapNotNull { locationType ->
            siteSelectionsByLocationTypeId[locationType.id]?.let { selectedLocationTypeSite ->
                locationType.name to selectedLocationTypeSite
            }
        }

        val selectedSites = selectedLocationHierarchy.indices.mapNotNull { selectedIndex ->
            val selectedLocationHierarchyAtLevel = selectedLocationHierarchy.take(selectedIndex + 1).toMap()
            allSites.find { site ->
                site.locationHierarchy?.filterValues { it.isNotBlank() } == selectedLocationHierarchyAtLevel
            }
        }

        return selectedSites.lastOrNull { it.latitude != null && it.longitude != null }
    }
}
