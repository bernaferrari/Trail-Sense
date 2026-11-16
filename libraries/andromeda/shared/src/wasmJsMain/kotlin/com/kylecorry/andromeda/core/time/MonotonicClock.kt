package com.kylecorry.andromeda.core.time

import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

internal actual fun monotonicNanos(): Long = startMark.elapsedNow().inWholeNanoseconds
