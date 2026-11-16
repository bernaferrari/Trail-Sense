package com.kylecorry.andromeda.core.system

public actual fun appInfo(): AppInfo = AppInfo(
    id = "com.kylecorry.trail_sense.web",
    name = "Trail Sense",
    version = "1.0",
    build = "1"
)

public actual fun deviceInfo(): DeviceInfo = DeviceInfo(
    platform = "Web",
    operatingSystemVersion = "Wasm",
    model = "Browser",
    manufacturer = null,
    apiLevel = null
)

public actual fun deviceMemory(): DeviceMemory = DeviceMemory(
    totalBytes = null,
    availableBytes = null,
    lowMemory = null
)
