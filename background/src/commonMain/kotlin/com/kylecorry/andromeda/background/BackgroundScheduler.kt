package com.kylecorry.andromeda.background

import kotlin.time.Instant

/** Requests platform-managed work no earlier than the specified time. */
interface BackgroundScheduler {
    fun schedule(earliest: Instant)
    fun cancel()
}
