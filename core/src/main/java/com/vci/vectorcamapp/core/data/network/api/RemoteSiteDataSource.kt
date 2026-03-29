package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.BuildConfig
import com.vci.vectorcamapp.core.data.dto.site.GetAllSitesResponseDto
import com.vci.vectorcamapp.core.data.dto.site.SiteDto
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

    companion object {
        private const val MAX_LIMIT = 100
    }

    override suspend fun getAllSitesForProgram(
        programId: Int
    ): Result<List<SiteDto>, NetworkError> {
        val allSites = mutableListOf<SiteDto>()
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val result = fetchSitesPage(programId, offset)

            if (result is Result.Error) {
                return Result.Error(result.error)
            }

            val response = (result as Result.Success).data
            allSites.addAll(response.sites)

            hasMore = response.hasMore
            offset += MAX_LIMIT
        }

        return Result.Success(allSites)
    }

    private suspend fun fetchSitesPage(
        programId: Int,
        offset: Int
    ): Result<GetAllSitesResponseDto, NetworkError> {
        return safeCall<GetAllSitesResponseDto> {
            httpClient.get(constructUrl("sites")) {
                bearerAuth(BuildConfig.VECTORCAM_API_KEY)
                contentType(ContentType.Application.Json)

                parameter("programId", programId)
                parameter("offset", offset)
                parameter("limit", MAX_LIMIT)
            }
        }
    }
}
