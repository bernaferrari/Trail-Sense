package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Duration.Companion.milliseconds

import kotlin.time.Duration

class ActiveTimeCalculator {

    fun calculate(steps: Long, elapsedTime: Duration): Duration {
        if (steps <= 0 || elapsedTime == Duration.ZERO || elapsedTime.isNegative()) {
            return Duration.ZERO
        }

        val elapsedMs = elapsedTime.inWholeMilliseconds
        val stepsPerMinute = steps.toDouble() * MILLIS_PER_MINUTE / elapsedMs

        return when {
            stepsPerMinute >= MIN_ACTIVE_STEPS_PER_MINUTE -> elapsedTime
            else -> minOf(elapsedTime, (steps * ACTIVE_MILLIS_PER_STEP).milliseconds)
        }
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MIN_ACTIVE_STEPS_PER_MINUTE = 30.0
        private const val ACTIVE_MILLIS_PER_STEP = 2_000L
    }
}
