package com.vci.vectorcamapp.imaging.domain.strategy.concrete

import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow

class SurveillanceImagingWorkflow : ImagingWorkflow {
    override val specimenFurtherProcessingProbability: Float
        get() = 0f

    override val allowModelInferenceToggle: Boolean
        get() = false
}
