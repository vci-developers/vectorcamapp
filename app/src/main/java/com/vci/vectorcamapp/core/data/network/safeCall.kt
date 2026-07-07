package com.vci.vectorcamapp.core.data.network

import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.domain.util.Result
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import timber.log.Timber
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, NetworkError> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        Timber.w(e, "Network: no internet (UnresolvedAddressException)")
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: UnknownHostException) {
        Timber.w(e, "Network: no internet (UnknownHostException)")
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: SerializationException) {
        Timber.e(e, "Network: serialization error")
        return Result.Error(NetworkError.SERIALIZATION_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        Timber.e(e, "Network: unexpected error")
        return Result.Error(NetworkError.UNKNOWN_ERROR)
    }

    return responseToResult(response)
}
