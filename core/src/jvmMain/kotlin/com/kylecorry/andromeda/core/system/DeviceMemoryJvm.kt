package com.kylecorry.andromeda.core.system

import java.lang.management.ManagementFactory

actual fun deviceMemory(): DeviceMemory {
    val os = ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean
    return DeviceMemory(os?.totalMemorySize, os?.freeMemorySize, null)
}
