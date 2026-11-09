package com.kylecorry.andromeda.core.system

public data class DeviceInfo(
    val platform: String,
    val operatingSystemVersion: String,
    val model: String?,
    val manufacturer: String?,
    val apiLevel: Int? = null,
)

public expect fun deviceInfo(): DeviceInfo
