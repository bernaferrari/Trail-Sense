package com.kylecorry.andromeda.core.system

import platform.Foundation.NSProcessInfo

public actual fun deviceMemory(): DeviceMemory {
    val total = NSProcessInfo.processInfo.physicalMemory.coerceAtMost(Long.MAX_VALUE.toULong()).toLong()
    return DeviceMemory(total, null, null)
}
