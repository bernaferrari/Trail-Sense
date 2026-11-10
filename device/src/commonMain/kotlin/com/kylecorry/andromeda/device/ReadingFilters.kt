package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Applies independent low-pass filters to the three vector components. */
public fun Flow<Reading<Vector3>>.lowPass(alpha: Float): Flow<Reading<Vector3>> = flow {
    require(alpha in 0f..1f)
    val x = LowPassFilter(alpha)
    val y = LowPassFilter(alpha)
    val z = LowPassFilter(alpha)
    collect { reading ->
        val value = reading.value
        emit(Reading(Vector3(x.filter(value.x), y.filter(value.y), z.filter(value.z)), reading.time))
    }
}
