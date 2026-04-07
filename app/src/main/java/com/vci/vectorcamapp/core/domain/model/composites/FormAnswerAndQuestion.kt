package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion

data class FormAnswerAndQuestion(
    val answer: FormAnswer,
    val question: FormQuestion
)
