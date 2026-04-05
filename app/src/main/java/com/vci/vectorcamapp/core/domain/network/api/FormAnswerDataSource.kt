package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.form_answer.GetFormAnswersResponseDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersResponseDto
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface FormAnswerDataSource {
    suspend fun postFormAnswersForSession(
        sessionId: Int, formVersion: String, answers: Map<Int, FormAnswer>
    ): Result<PostFormAnswersResponseDto, NetworkError>

    suspend fun getFormAnswersForSession(sessionId: Int): Result<GetFormAnswersResponseDto, NetworkError>
}
