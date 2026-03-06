package com.vci.vectorcamapp.imaging.domain.strategy

import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.imaging.domain.strategy.concrete.DataCollectionImagingWorkflow
import com.vci.vectorcamapp.imaging.domain.strategy.concrete.SurveillanceImagingWorkflow
import javax.inject.Inject

class ImagingWorkflowFactory @Inject constructor() {
    fun create(sessionType: SessionType): ImagingWorkflow {
        return when (sessionType) {
            SessionType.SURVEILLANCE -> SurveillanceImagingWorkflow()
            SessionType.DATA_COLLECTION -> DataCollectionImagingWorkflow()
        }
    }
}
