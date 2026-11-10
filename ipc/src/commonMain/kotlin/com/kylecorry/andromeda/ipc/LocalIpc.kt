@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kylecorry.andromeda.ipc

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.port
import io.ktor.utils.io.readLineStrict
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val LOOPBACK = "127.0.0.1"
private const val DEFAULT_MAX_FRAME = 1_048_576

/** A client for a token-protected server listening on the local loopback interface. */
public class LocalIpcClient(
    private val port: Int,
    private val token: String,
    private val timeout: Duration = 30.seconds,
    private val maxFrameBytes: Int = DEFAULT_MAX_FRAME,
) : IpcClient {
    init {
        require(port in 1..65535 && token.isNotEmpty())
        require(timeout > Duration.ZERO && timeout.isFinite() && maxFrameBytes > 0)
    }

    override suspend fun send(path: String, request: IpcRequest): IpcResponse = withTimeout(timeout) {
        val selector = SelectorManager(Dispatchers.IO)
        try {
            val socket = aSocket(selector).tcp().connect(LOOPBACK, port)
            try {
                val message = encodeRequest(token, path, request)
                require(message.encodeToByteArray().size <= maxFrameBytes) { "IPC request exceeds the frame limit" }
                socket.openWriteChannel(autoFlush = true).writeStringUtf8(message + "\n")
                val response = socket.openReadChannel().readLineStrict(maxFrameBytes.toLong())
                    ?: error("IPC server closed without a response")
                decodeResponse(response)
            } finally {
                socket.close()
            }
        } finally {
            selector.close()
        }
    }
}

/** A loopback server owned by the caller; close it to stop accepting and cancel active requests. */
public class LocalIpcServer internal constructor(
    public val port: Int,
    private val stop: () -> Unit,
) : AutoCloseable {
    override fun close(): Unit = stop()
}

/**
 * Starts a token-protected loopback server. The app must share [token] with trusted clients using
 * a platform-appropriate mechanism. Loopback TCP does not provide OS identity or encryption.
 */
public suspend fun localIpcServer(
    router: IpcClient,
    token: String,
    scope: CoroutineScope,
    port: Int = 0,
    timeout: Duration = 30.seconds,
    maxFrameBytes: Int = DEFAULT_MAX_FRAME,
    maxConnections: Int = 16,
): LocalIpcServer {
    require(token.isNotEmpty() && port in 0..65535)
    require(timeout > Duration.ZERO && timeout.isFinite() && maxFrameBytes > 0 && maxConnections > 0)
    val selector = SelectorManager(Dispatchers.IO)
    val server = try { aSocket(selector).tcp().bind(LOOPBACK, port) } catch (error: Throwable) {
        selector.close()
        throw error
    }
    val job = SupervisorJob(scope.coroutineContext[Job])
    val serverScope = CoroutineScope(scope.coroutineContext + job)
    val permits = Semaphore(maxConnections)
    val acceptJob = serverScope.launch {
        while (isActive) {
            permits.acquire()
            val socket = try { server.accept() } catch (error: Throwable) {
                permits.release()
                throw error
            }
            launch {
                try {
                    withTimeout(timeout) {
                        val input = socket.openReadChannel().readLineStrict(maxFrameBytes.toLong())
                            ?: error("IPC client closed without a request")
                        val parsed = try { decodeRequest(input) } catch (error: Exception) { null }
                        val response = when {
                            parsed == null -> IpcResponse(CODE_BAD_REQUEST)
                            parsed.token != token -> IpcResponse(CODE_UNAUTHORIZED)
                            else -> try {
                                router.send(parsed.path, parsed.request)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                IpcResponse(CODE_INTERNAL_ERROR)
                            }
                        }
                        val encoded = encodeResponse(response)
                        require(encoded.encodeToByteArray().size <= maxFrameBytes) { "IPC response exceeds the frame limit" }
                        socket.openWriteChannel(autoFlush = true).writeStringUtf8(encoded + "\n")
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A malformed, disconnected or oversized client must not stop the server.
                } finally {
                    socket.close()
                    permits.release()
                }
            }
        }
    }
    acceptJob.invokeOnCompletion {
        job.cancel()
        server.close()
        selector.close()
    }
    return LocalIpcServer(server.port) {
        job.cancel()
        server.close()
        selector.close()
    }
}

private data class WireRequest(val token: String, val path: String, val request: IpcRequest)

private fun encodeRequest(token: String, path: String, request: IpcRequest): String = buildJsonObject {
    put("token", token)
    put("path", path)
    put("headers", encodeHeaders(request.headers))
    put("payload", request.payload?.let { JsonPrimitive(Base64.encode(it)) } ?: JsonNull)
}.toString()

private fun encodeResponse(response: IpcResponse): String = buildJsonObject {
    put("code", response.code)
    put("headers", encodeHeaders(response.headers))
    put("payload", response.payload?.let { JsonPrimitive(Base64.encode(it)) } ?: JsonNull)
}.toString()

private fun encodeHeaders(headers: Map<String, IpcValue>): JsonObject = buildJsonObject {
    for ((key, value) in headers) put(key, buildJsonObject {
        when (value) {
            IpcValue.Null -> put("type", "null")
            is IpcValue.Text -> { put("type", "text"); put("value", value.value) }
            is IpcValue.Bool -> { put("type", "bool"); put("value", value.value.toString()) }
            is IpcValue.Int32 -> { put("type", "int32"); put("value", value.value.toString()) }
            is IpcValue.Int64 -> { put("type", "int64"); put("value", value.value.toString()) }
            is IpcValue.Float32 -> { put("type", "float32"); put("value", value.value.toString()) }
            is IpcValue.Float64 -> { put("type", "float64"); put("value", value.value.toString()) }
            is IpcValue.Bytes -> { put("type", "bytes"); put("value", Base64.encode(value.value)) }
        }
    })
}

private fun decodeHeaders(headers: JsonObject): Map<String, IpcValue> = headers.mapValues { (_, element) ->
    val obj = element.jsonObject
    val value = obj["value"]?.jsonPrimitive?.content
    when (obj.getValue("type").jsonPrimitive.content) {
        "null" -> IpcValue.Null
        "text" -> IpcValue.Text(requireNotNull(value))
        "bool" -> IpcValue.Bool(requireNotNull(value).toBooleanStrict())
        "int32" -> IpcValue.Int32(requireNotNull(value).toInt())
        "int64" -> IpcValue.Int64(requireNotNull(value).toLong())
        "float32" -> IpcValue.Float32(requireNotNull(value).toFloat())
        "float64" -> IpcValue.Float64(requireNotNull(value).toDouble())
        "bytes" -> IpcValue.Bytes(Base64.decode(requireNotNull(value)))
        else -> error("Unknown IPC value type")
    }
}

private fun decodePayload(value: JsonObject): ByteArray? =
    value.getValue("payload").jsonPrimitive.contentOrNull?.let(Base64::decode)

private fun decodeRequest(text: String): WireRequest {
    val value = Json.parseToJsonElement(text).jsonObject
    return WireRequest(
        value.getValue("token").jsonPrimitive.content,
        value.getValue("path").jsonPrimitive.content,
        IpcRequest(decodeHeaders(value.getValue("headers").jsonObject), decodePayload(value)),
    )
}

private fun decodeResponse(text: String): IpcResponse {
    val value = Json.parseToJsonElement(text).jsonObject
    return IpcResponse(value.getValue("code").jsonPrimitive.int, decodeHeaders(value.getValue("headers").jsonObject), decodePayload(value))
}
