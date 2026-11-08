package com.kylecorry.andromeda.core.system

actual fun deviceInfo(): DeviceInfo = DeviceInfo(
    System.getProperty("os.name", "Unknown"),
    System.getProperty("os.version", "Unknown"),
    null,
    null,
)
