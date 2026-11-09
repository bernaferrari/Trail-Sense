package com.kylecorry.sol.science.oceanography.waterlevel

import java.time.ZonedDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.toKotlinInstant

internal fun IWaterLevelCalculator.calculate(time: ZonedDateTime): Float =
    calculate(time.toInstant().toKotlinInstant(), TimeZone.of(time.zone.id))
