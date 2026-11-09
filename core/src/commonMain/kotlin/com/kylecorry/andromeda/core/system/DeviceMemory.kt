package com.kylecorry.andromeda.core.system

/** Physical memory information. Null fields are not exposed by the platform. */
public data class DeviceMemory(val totalBytes: Long?, val availableBytes: Long?, val lowMemory: Boolean?)

/** Reads current memory information. Android requires KSensor context initialization. */
public expect fun deviceMemory(): DeviceMemory
