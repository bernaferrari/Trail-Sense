package com.kylecorry.andromeda.core.system

public actual fun deviceInfo(): DeviceInfo = DeviceInfo(
    System.getProperty("os.name", "Unknown"),
    System.getProperty("os.version", "Unknown"),
    null,
    null,
)
