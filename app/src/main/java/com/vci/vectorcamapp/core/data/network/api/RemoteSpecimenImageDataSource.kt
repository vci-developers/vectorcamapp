package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.specimen_image.PostSpecimenImageResponseDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.SpecimenImageDto
import com.vci.vectorcamapp.core.data.mappers.toPostSpecimenImageRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.network.api.SpecimenImageDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteSpecimenImageDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SpecimenImageDataSource {
    override suspend fun postSpecimenImageMetadata(
        specimenImage: SpecimenImage, inferenceResult: InferenceResult?, specimenId: Int
    ): Result<PostSpecimenImageResponseDto, NetworkError> {
        return safeCall<PostSpecimenImageResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.specimenImagesData(specimenId))) {
                setBody(specimenImage.toPostSpecimenImageRequestDto(inferenceResult))
            }
        }
    }

    override suspend fun getSpecimenImageMetadata(
        specimenImageId: String, specimenId: Int
    ): Result<SpecimenImageDto, NetworkError> {
        return safeCall<SpecimenImageDto> {
            httpClient.get(constructUrl("specimens/${specimenId}/images/data/${specimenImageId}"))
        }
    }
}