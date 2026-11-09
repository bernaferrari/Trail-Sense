package com.kylecorry.andromeda.ipc

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalIpcTest {
    @Test
    fun exchangesTypedMessagesAndRejectsWrongToken() = runBlocking {
        val router = IpcRouter(mapOf("echo" to { request ->
            IpcResponse(CODE_OK, request.headers, request.payload)
        }))
        val server = localIpcServer(router, "test-token", this)
        try {
            val client = LocalIpcClient(server.port, "test-token")
            val headers = mapOf(
                "null" to IpcValue.Null,
                "text" to IpcValue.Text("hello\nworld"),
                "bool" to IpcValue.Bool(true),
                "int" to IpcValue.Int32(-42),
                "long" to IpcValue.Int64(Long.MAX_VALUE),
                "float" to IpcValue.Float32(1.5f),
                "double" to IpcValue.Float64(-2.75),
                "bytes" to IpcValue.Bytes(byteArrayOf(0, 1, -1)),
            )
            val payload = byteArrayOf(0, 1, 2, -1)
            val response = client.send("echo", IpcRequest(headers, payload))
            assertEquals(CODE_OK, response.code)
            assertEquals(headers, response.headers)
            assertContentEquals(payload, response.payload)
            assertEquals(CODE_NOT_FOUND, client.send("missing").code)
            assertEquals(CODE_UNAUTHORIZED, LocalIpcClient(server.port, "wrong").send("echo").code)
            assertFailsWith<IllegalArgumentException> {
                LocalIpcClient(server.port, "test-token", maxFrameBytes = 8).send("echo")
            }
        } finally {
            server.close()
        }
        Unit
    }
}
