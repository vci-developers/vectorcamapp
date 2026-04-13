package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.Form

data class FormWithFormAnswersAndQuestions(
    val form: Form,
    val formAnswersAndQuestions: List<FormAnswerAndQuestion>
)
