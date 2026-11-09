package com.kylecorry.trail_sense.tools.battery.domain

import kotlin.time.Duration

data class RunningService(
    val name: String,
    val frequency: Duration,
    val disable: suspend () -> Unit
)
