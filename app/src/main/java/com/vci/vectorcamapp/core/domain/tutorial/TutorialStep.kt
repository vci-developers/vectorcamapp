package com.vci.vectorcamapp.core.domain.tutorial

enum class TutorialStep(val stepIndex: Int) {
    REGISTRATION_FORM(1),
    NEW_SURVEILLANCE_SESSION(2),
    INTAKE_FORM(3),
    CAPTURE_AND_SAVE(4),
    IN_PROGRESS_SESSIONS(5),
    COMPLETE_SESSIONS(6),
    COMPLETED(7);

    companion object {
        const val TOTAL_STEPS = 6
    }

    val isLast get() = this == COMPLETE_SESSIONS
    val isCompleted get() = this == COMPLETED

    fun next(): TutorialStep = when (this) {
        REGISTRATION_FORM -> NEW_SURVEILLANCE_SESSION
        NEW_SURVEILLANCE_SESSION -> INTAKE_FORM
        INTAKE_FORM -> CAPTURE_AND_SAVE
        CAPTURE_AND_SAVE -> IN_PROGRESS_SESSIONS
        IN_PROGRESS_SESSIONS -> COMPLETE_SESSIONS
        COMPLETE_SESSIONS -> COMPLETED
        COMPLETED -> COMPLETED
    }
}
