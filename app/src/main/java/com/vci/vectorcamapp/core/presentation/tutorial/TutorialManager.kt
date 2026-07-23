package com.vci.vectorcamapp.core.presentation.tutorial

import com.vci.vectorcamapp.core.domain.tutorial.TutorialRepository
import com.vci.vectorcamapp.core.domain.tutorial.TutorialStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorialManager @Inject constructor(
    private val repository: TutorialRepository
) {
    private val _currentStep = MutableStateFlow(repository.getCurrentStep())
    val currentStep: StateFlow<TutorialStep> = _currentStep.asStateFlow()

    fun advanceStep() {
        val next = _currentStep.value.next()
        _currentStep.value = next
        repository.saveStep(next)
    }

    fun skipTutorial() {
        _currentStep.value = TutorialStep.COMPLETED
        repository.saveStep(TutorialStep.COMPLETED)
    }

    fun resetTutorial() {
        _currentStep.value = TutorialStep.REGISTRATION_FORM
        repository.saveStep(TutorialStep.REGISTRATION_FORM)
    }

    fun restartTutorial() {
        _currentStep.value = TutorialStep.NEW_SURVEILLANCE_SESSION
        repository.saveStep(TutorialStep.NEW_SURVEILLANCE_SESSION)
    }
}
