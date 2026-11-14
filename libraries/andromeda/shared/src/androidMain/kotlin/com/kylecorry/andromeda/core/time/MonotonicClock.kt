package com.kylecorry.andromeda.core.time

internal actual fun monotonicNanos(): Long = android.os.SystemClock.elapsedRealtimeNanos()
