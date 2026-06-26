package com.vci.vectorcamapp.core.domain.tutorial

interface TutorialRepository {
    fun getCurrentStep(): TutorialStep
    fun saveStep(step: TutorialStep)
}
