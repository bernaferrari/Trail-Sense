package com.kylecorry.trail_sense.tools.navigation.domain.astronomy

import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase

data class NavAstronomyData(
    val sunBearing: Float,
    val moonBearing: Float,
    val isSunUp: Boolean,
    val isMoonUp: Boolean,
    val moonPhase: MoonTruePhase,
    val moonTilt: Float
)
