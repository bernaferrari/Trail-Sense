package com.kylecorry.trail_sense.tools.battery.domain

import kotlin.time.Duration
import kotlin.time.Instant

data class BatteryReading(
    val time: Instant,
    val percent: Float,
    val capacity: Float,
    val isCharging: Boolean,
    val uptime: Duration? = null,
    val elapsedRealtime: Duration? = null
)
