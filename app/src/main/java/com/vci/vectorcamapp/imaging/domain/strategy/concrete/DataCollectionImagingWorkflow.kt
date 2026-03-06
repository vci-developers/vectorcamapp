package com.vci.vectorcamapp.imaging.domain.strategy.concrete

import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow

class DataCollectionImagingWorkflow : ImagingWorkflow {
    override val specimenFurtherProcessingProbability: Float
        get() = 0f

    override val allowModelInferenceToggle: Boolean
        get() = true

    override val isPracticeSession: Boolean
        get() = false
}
