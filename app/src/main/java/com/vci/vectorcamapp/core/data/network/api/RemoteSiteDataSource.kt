package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.dto.site.GetAllSitesResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class RemoteSiteDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SiteDataSource {

    override suspend fun getSitesForProgram(
        programId: Int
    ): Result<GetAllSitesResponseDto, NetworkError> {
        return safeCall<GetAllSitesResponseDto> {
            httpClient.get(constructUrl("sites")) {
                bearerAuth(BuildConfig.VECTORCAM_API_KEY)
                contentType(ContentType.Application.Json)

                parameter("programId", programId)
            }
        }
    }
}
