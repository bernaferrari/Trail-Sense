package com.kylecorry.andromeda.net

import io.ktor.client.HttpClient as KtorClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod as KtorMethod
import kotlin.time.Duration

class HttpClient : AutoCloseable {
    private val following = client(true)
    private val notFollowing = client(false)

    suspend fun send(
        url: String,
        method: HttpMethod = HttpMethod.GET,
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap(),
        readTimeout: Duration? = null,
        connectTimeout: Duration? = null,
        followRedirects: Boolean = true
    ): HttpResponse {
        val client = if (followRedirects) following else notFollowing
        val response = client.request(url) {
            this.method = KtorMethod.parse(method.name)
            headers.forEach { (name, value) -> this.headers.append(name, value) }
            if (body != null) setBody(body)
            timeout {
                readTimeout?.let { socketTimeoutMillis = it.inWholeMilliseconds }
                connectTimeout?.let { connectTimeoutMillis = it.inWholeMilliseconds }
            }
        }
        return HttpResponse(response.status.value, response.headers.entries().associate { it.key to it.value }, response.body())
    }

    override fun close() {
        following.close()
        notFollowing.close()
    }

    private fun client(redirects: Boolean) = KtorClient {
        followRedirects = redirects
        install(HttpTimeout)
    }
}
