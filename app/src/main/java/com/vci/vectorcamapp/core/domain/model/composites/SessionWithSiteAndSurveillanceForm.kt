package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm

data class SessionWithSiteAndSurveillanceForm(
    val session: Session,
    val site: Site,
    val surveillanceForm: SurveillanceForm
)