package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.BuildConfig
import com.vci.vectorcamapp.core.data.dto.location_type.GetAllLocationTypesResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class RemoteLocationTypeDataSource @Inject constructor(
    private val httpClient: HttpClient
) : LocationTypeDataSource {
    override suspend fun getAllLocationTypesForProgram(programId: Int): Result<GetAllLocationTypesResponseDto, NetworkError> {
        return safeCall<GetAllLocationTypesResponseDto> {
            httpClient.get(constructUrl("programs/$programId/location-types")) {
                bearerAuth(BuildConfig.VECTORCAM_API_KEY)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
