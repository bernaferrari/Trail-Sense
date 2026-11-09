package com.kylecorry.andromeda.core.system

data class DeviceInfo(
    val platform: String,
    val operatingSystemVersion: String,
    val model: String?,
    val manufacturer: String?,
    val apiLevel: Int? = null,
)

expect fun deviceInfo(): DeviceInfo
