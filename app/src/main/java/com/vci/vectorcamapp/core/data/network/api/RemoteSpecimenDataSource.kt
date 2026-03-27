package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenRequestDto
import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenResponseDto
import com.vci.vectorcamapp.core.data.dto.specimen.SpecimenDto
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
import javax.inject.Inject

class RemoteSpecimenDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SpecimenDataSource {

    override suspend fun postSpecimen(
        specimen: Specimen, sessionId: Int, expectedImages: Int
    ): Result<PostSpecimenResponseDto, NetworkError> {
        return safeCall<PostSpecimenResponseDto> {
            httpClient.post(constructUrl("sessions/$sessionId/specimens")) {
                setBody(
                    PostSpecimenRequestDto(
                        specimenId = specimen.id,
                        shouldProcessFurther = specimen.shouldProcessFurther,
                        expectedImages = expectedImages
                    )
                )
            }
        }
    }

    override suspend fun getSpecimenByIdAndSessionId(specimenId: String, sessionId: Int): Result<SpecimenDto, NetworkError> {
        return safeCall<SpecimenDto> {
            httpClient.get(constructUrl("sessions/$sessionId/specimens/$specimenId"))
        }
    }
}
