package com.kylecorry.andromeda.permissions

public data class PermissionRationale(
    val title: String,
    val message: CharSequence,
    val ok: String? = null,
    val cancel: String? = null
)
