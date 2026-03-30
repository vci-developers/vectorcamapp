package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.form_answer.FormAnswerRequestItemDto
import com.vci.vectorcamapp.core.data.dto.form_answer.GetFormAnswersResponseDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersRequestDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.network.api.FormAnswerDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteFormAnswerDataSource @Inject constructor(
    private val httpClient: HttpClient
): FormAnswerDataSource {
    override suspend fun postFormAnswersForSession(
        sessionId: Int,
        formVersion: String,
        answers: Map<Int, FormAnswer>
    ): Result<PostFormAnswersResponseDto, NetworkError> {
        return safeCall<PostFormAnswersResponseDto> {
            httpClient.post(constructUrl("sessions/$sessionId/forms/answers")) {
                setBody(PostFormAnswersRequestDto(
                    answers = answers.map { (questionId, answer) ->
                        FormAnswerRequestItemDto(
                            frontendId = answer.localId,
                            questionId = questionId,
                            value = answer.value,
                            dataType = answer.dataType
                        )
                    },
                    formVersion = formVersion,
                    submittedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    override suspend fun getFormAnswersForSession(sessionId: Int): Result<GetFormAnswersResponseDto, NetworkError> {
        return safeCall<GetFormAnswersResponseDto> {
            httpClient.get(constructUrl("sessions/$sessionId/forms/answers"))
        }
    }

}