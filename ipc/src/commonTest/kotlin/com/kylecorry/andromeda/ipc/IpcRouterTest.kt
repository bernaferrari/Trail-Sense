package com.kylecorry.andromeda.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IpcRouterTest {
    @Test
    fun dispatchesRequestsAndPropagatesCancellation() = runBlocking {
        var failure: Exception? = null
        val client: IpcClient = IpcRouter(
            mapOf(
                "echo" to { request -> InterprocessCommunicationResponse(CODE_OK, request.headers, request.payload) },
                "fail" to { throw IllegalStateException("failed") },
                "cancel" to { throw CancellationException("cancelled") },
            ),
            onError = { failure = it },
        )
        val request = InterprocessCommunicationRequest(mapOf("name" to IpcValue.Text("value")), byteArrayOf(1, 2))
        val response = client.send("echo", request)
        assertEquals(CODE_OK, response.code)
        assertEquals(request.headers, response.headers)
        assertContentEquals(request.payload, response.payload)
        assertEquals(CODE_NOT_FOUND, client.send("missing").code)
        assertEquals(CODE_INTERNAL_ERROR, client.send("fail").code)
        assertEquals("failed", failure?.message)
        assertFailsWith<CancellationException> { client.send("cancel") }
        Unit
    }
}
