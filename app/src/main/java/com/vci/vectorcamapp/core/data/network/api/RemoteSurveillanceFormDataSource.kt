package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.surveillance_form.PostSurveillanceFormResponseDto
import com.vci.vectorcamapp.core.data.dto.surveillance_form.SurveillanceFormDto
import com.vci.vectorcamapp.core.data.mappers.toPostSurveillanceFormRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.network.api.SurveillanceFormDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteSurveillanceFormDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SurveillanceFormDataSource {

    override suspend fun postSurveillanceForm(
        surveillanceForm: SurveillanceForm,
        sessionId: Int
    ): Result<PostSurveillanceFormResponseDto, NetworkError> {
        return safeCall<PostSurveillanceFormResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.surveillanceForm(sessionId))) {
                setBody(surveillanceForm.toPostSurveillanceFormRequestDto(sessionId))
            }
        }
    }

    override suspend fun getSurveillanceFormBySessionId(sessionId: Int): Result<SurveillanceFormDto, NetworkError> {
        return safeCall<SurveillanceFormDto> {
            httpClient.get(constructUrl("sessions/$sessionId/survey"))
        }
    }
}
