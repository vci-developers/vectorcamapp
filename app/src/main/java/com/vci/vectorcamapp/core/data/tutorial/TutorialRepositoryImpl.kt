package com.vci.vectorcamapp.core.data.tutorial

import android.content.Context
import com.vci.vectorcamapp.core.domain.tutorial.TutorialRepository
import com.vci.vectorcamapp.core.domain.tutorial.TutorialStep
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TutorialRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TutorialRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getCurrentStep(): TutorialStep {
        val stepName = prefs.getString(KEY_STEP, TutorialStep.REGISTRATION_FORM.name)
        return TutorialStep.entries.firstOrNull { it.name == stepName }
            ?: TutorialStep.REGISTRATION_FORM
    }

    override fun saveStep(step: TutorialStep) {
        prefs.edit().putString(KEY_STEP, step.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "tutorial_prefs"
        private const val KEY_STEP = "tutorial_step"
    }
}
