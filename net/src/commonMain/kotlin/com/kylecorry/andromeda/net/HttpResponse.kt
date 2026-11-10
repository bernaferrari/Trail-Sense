package com.kylecorry.andromeda.net

public class HttpResponse(
    public val code: Int,
    public val headers: Map<String, List<String>>,
    public val content: ByteArray?
) {
    public fun contentAsString(): String? {
        return content?.decodeToString()
    }

    public fun isSuccessful(): Boolean {
        return code in 200..299
    }
}