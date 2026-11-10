package com.kylecorry.andromeda.files

import android.net.Uri
import kotlin.time.Instant

public data class ContentDocument(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val canRead: Boolean,
    val canWrite: Boolean,
    val lastModified: Instant,
    val size: Long
)