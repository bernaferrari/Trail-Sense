package com.kylecorry.andromeda.core.system

import android.app.ActivityManager
import android.content.Context
import com.ksensor.core.context.KSensorContext

public actual fun deviceMemory(): DeviceMemory {
    val manager = KSensorContext.get().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return DeviceMemory(null, null, null)
    val memory = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memory)
    return DeviceMemory(memory.totalMem, memory.availMem, memory.lowMemory)
}
