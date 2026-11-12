package com.kylecorry.trail_sense.main.errors

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeModeTest {

    @Test
    fun threeStartsWithinThirtySecondsEachIsACrashLoop() {
        assertTrue(SafeMode.isCrashLoop(now = 70_000, lastStartTime = 50_000, lastMinus1StartTime = 30_000))
        assertTrue(SafeMode.isCrashLoop(now = 60_000, lastStartTime = 30_000, lastMinus1StartTime = 0))
    }

    @Test
    fun aSlowRestartIsNot() {
        assertFalse(SafeMode.isCrashLoop(now = 100_000, lastStartTime = 50_000, lastMinus1StartTime = 40_000))
        assertFalse(SafeMode.isCrashLoop(now = 60_000, lastStartTime = 50_000, lastMinus1StartTime = 10_000))
    }
}
