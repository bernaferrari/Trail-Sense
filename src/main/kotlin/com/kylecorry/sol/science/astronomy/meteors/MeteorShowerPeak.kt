package com.kylecorry.sol.science.astronomy.meteors

import java.time.ZonedDateTime

data class MeteorShowerPeak(
    val shower: MeteorShower,
    val start: ZonedDateTime,
    val peak: ZonedDateTime,
    val end: ZonedDateTime
)
