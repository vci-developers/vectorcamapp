package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.network.api.FormAnswerDataSource
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import javax.inject.Inject

class RemoteFormAnswerDataSource @Inject constructor(
    private val httpClient: HttpClient
): FormAnswerDataSource {

}