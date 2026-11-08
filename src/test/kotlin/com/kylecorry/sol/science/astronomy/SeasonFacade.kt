package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

internal object SeasonFacade {
    fun getSeason(location: Coordinate, date: ZonedDateTime): Season =
        SeasonCalculator.getSeason(location, date.toInstant().toKotlinInstant())
}
