package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.tests.ZonedTime

internal fun IWaterLevelCalculator.calculate(time: ZonedTime): Float = calculate(time.instant, time.zone)
