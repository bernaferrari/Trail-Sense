package com.kylecorry.trail_sense.shared.hooks

import kotlinx.atomicfu.locks.synchronized

import kotlinx.atomicfu.locks.SynchronizedObject

import kotlin.time.Clock

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Duration

class HookTriggers {
    private val distanceTriggers = mutableMapOf<String, DistanceHookTrigger>()
    private val distanceLock = SynchronizedObject()

    private val frequencyTriggers = mutableMapOf<String, FrequencyHookTrigger>()
    private val frequencyLock = SynchronizedObject()

    fun distance(
        name: String,
        location: Coordinate,
        threshold: Distance,
        highAccuracy: Boolean = true
    ): Boolean {
        val conditional = synchronized(distanceLock) {
            distanceTriggers.getOrPut(name) { DistanceHookTrigger() }
        }
        return conditional.getValue(location, threshold, highAccuracy)
    }

    fun frequency(name: String, threshold: Duration): Boolean {
        val conditional = synchronized(frequencyLock) {
            frequencyTriggers.getOrPut(name) { FrequencyHookTrigger() }
        }
        return conditional.getValue(Clock.System.now(), threshold)
    }

}
