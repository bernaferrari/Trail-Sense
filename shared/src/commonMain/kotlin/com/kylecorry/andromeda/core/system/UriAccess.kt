package com.kylecorry.andromeda.core.system

public data class UriAccess(
    val requirePersistentAccess: Boolean = false,
    val requireReadAccess: Boolean = true,
    val requireWriteAccess: Boolean = false
)
