package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenResponseDto
import com.vci.vectorcamapp.core.data.dto.specimen.SpecimenDto
import com.vci.vectorcamapp.core.data.mappers.toPostSpecimenRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.network.api.SpecimenDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import timber.log.Timber
import javax.inject.Inject

class RemoteSpecimenDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SpecimenDataSource {

    override suspend fun postSpecimen(
        specimen: Specimen, sessionId: Int, sessionUnitId: Int?, expectedImages: Int
    ): Result<PostSpecimenResponseDto, NetworkError> {
        val body = specimen.toPostSpecimenRequestDto(sessionUnitId, expectedImages)
        Timber.tag(TAG).i(
            "POST ${UploadRoutes.specimens(sessionId)} specimenId=${body.specimenId} " +
                "sessionUnitId=${body.sessionUnitId}"
        )
        val result = safeCall<PostSpecimenResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.specimens(sessionId))) {
                setBody(body)
            }
        }
        Timber.tag(TAG).i("POST specimen ${specimen.id} result=$result")
        return result
    }

    override suspend fun getSpecimenByIdAndSessionId(specimenId: String, sessionId: Int): Result<SpecimenDto, NetworkError> {
        val result = safeCall<SpecimenDto> {
            httpClient.get(constructUrl("sessions/$sessionId/specimens/$specimenId"))
        }
        Timber.tag(TAG).i("GET specimen $specimenId session=$sessionId result=$result")
        return result
    }

    companion object {
        private const val TAG = "DataRecovery"
    }
}
