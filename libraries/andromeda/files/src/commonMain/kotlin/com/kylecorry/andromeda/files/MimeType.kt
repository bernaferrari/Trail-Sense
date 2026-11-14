package com.kylecorry.andromeda.files

import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import io.ktor.http.fromFileExtension

/** Looks up media types and filename extensions using Ktor's multiplatform registry. */
public object MimeType {
    public fun toExtension(mimeType: String): String? =
        runCatching { ContentType.parse(mimeType).withoutParameters().fileExtensions().firstOrNull() }.getOrNull()

    public fun toMimeType(extension: String): String? =
        ContentType.fromFileExtension(extension.removePrefix(".").lowercase()).firstOrNull()?.toString()
}
