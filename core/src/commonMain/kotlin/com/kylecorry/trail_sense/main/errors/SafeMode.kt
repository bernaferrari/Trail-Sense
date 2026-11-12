package com.kylecorry.trail_sense.main.errors

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.logging.Logger
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/**
 * After the app starts three times within a short span (likely a crash loop), heavy optional work such as map
 * layers and plugins stays off for a little while.
 */
object SafeMode {

    private const val TAG = "SafeMode"
    private const val RESTART_CRASH_THRESHOLD_MILLIS = 30 * 1000L
    private const val SAFE_MODE_DURATION_MILLIS = 20 * 1000L

    @Volatile
    private var safeModeEndTime: Long = 0L

    /**
     * Records an app start, given the two starts before it (epoch milliseconds, 0 when unknown).
     */
    fun onAppStarted(
        lastStartTime: Long,
        lastMinus1StartTime: Long,
        now: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        safeModeEndTime = if (isCrashLoop(now, lastStartTime, lastMinus1StartTime)) {
            getAppService<Logger>().warn(
                TAG,
                "App restarted twice within ${RESTART_CRASH_THRESHOLD_MILLIS / 1000}s, entering safe mode"
            )
            now + SAFE_MODE_DURATION_MILLIS
        } else {
            0L
        }
    }

    fun isEnabled(): Boolean {
        return !isDebug() && safeModeEndTime > 0 &&
                Clock.System.now().toEpochMilliseconds() < safeModeEndTime
    }

    internal fun isCrashLoop(now: Long, lastStartTime: Long, lastMinus1StartTime: Long): Boolean {
        return now - lastStartTime <= RESTART_CRASH_THRESHOLD_MILLIS &&
                lastStartTime - lastMinus1StartTime <= RESTART_CRASH_THRESHOLD_MILLIS
    }
}
