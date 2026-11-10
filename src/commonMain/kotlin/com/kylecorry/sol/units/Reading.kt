package com.kylecorry.sol.units

import kotlin.time.Instant

public data class Reading<T>(val value: T, val time: Instant)
