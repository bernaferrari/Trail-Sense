package com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FeatureState
import kotlinx.coroutines.flow.StateFlow

interface IPedometerSubsystem {
    val steps: StateFlow<Long>
    val distance: StateFlow<Distance>
    val state: StateFlow<FeatureState>

    fun enable()
    fun disable()

    /** Whether the pedometer is off only because the activity permission is missing. */
    fun isDisabledDueToPermissions(): Boolean
}
