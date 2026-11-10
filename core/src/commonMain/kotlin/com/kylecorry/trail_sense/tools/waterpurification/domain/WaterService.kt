package com.kylecorry.trail_sense.tools.waterpurification.domain

import kotlin.time.Duration.Companion.minutes

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import kotlin.time.Duration

class WaterService : IWaterService {

    override fun getPurificationTime(altitude: Distance?): Duration {
        if (altitude == null || altitude.convertTo(DistanceUnits.Meters).value >= 1000f) {
            return 3.minutes
        }

        return 1.minutes
    }

}
