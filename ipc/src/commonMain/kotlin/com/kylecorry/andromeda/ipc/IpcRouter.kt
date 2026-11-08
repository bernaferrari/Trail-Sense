package com.kylecorry.andromeda.ipc

import kotlinx.coroutines.CancellationException

/** A transport capable of sending an IPC request to a named route. */
fun interface IpcClient {
    suspend fun send(
        path: String,
        request: InterprocessCommunicationRequest,
    ): InterprocessCommunicationResponse
}

/** Dispatches requests to shared suspend handlers. Handlers may capture platform services. */
class IpcRouter(
    private val routes: Map<String, suspend (InterprocessCommunicationRequest) -> InterprocessCommunicationResponse>,
    private val onError: (Exception) -> Unit = {},
) : IpcClient {
    override suspend fun send(path: String, request: InterprocessCommunicationRequest): InterprocessCommunicationResponse {
        val handler = routes[path] ?: return InterprocessCommunicationResponse(CODE_NOT_FOUND)
        return try {
            handler(request)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            onError(error)
            InterprocessCommunicationResponse(CODE_INTERNAL_ERROR)
        }
    }
}

/** Sends a request without headers or a payload. */
suspend fun IpcClient.send(path: String): InterprocessCommunicationResponse =
    send(path, InterprocessCommunicationRequest())
