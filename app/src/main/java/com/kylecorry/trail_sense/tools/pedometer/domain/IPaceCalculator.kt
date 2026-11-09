package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import kotlin.time.Duration

interface IPaceCalculator {
    fun distance(steps: Long): Distance
    fun speed(steps: Long, time: Duration): Speed
}
