package com.vci.vectorcamapp.imaging.domain.strategy

interface ImagingWorkflow {
    val specimenFurtherProcessingProbability: Float
    val allowModelInferenceToggle: Boolean
    val isPracticeSession: Boolean
}
