package com.kylecorry.sol.science.astronomy.meteors

import kotlin.time.Instant

public data class MeteorShowerPeak(val shower: MeteorShower, val start: Instant, val peak: Instant, val end: Instant)
