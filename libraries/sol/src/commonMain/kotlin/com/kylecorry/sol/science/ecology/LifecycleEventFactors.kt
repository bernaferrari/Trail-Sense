package com.kylecorry.sol.science.ecology

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Temperature
import kotlin.time.Duration
import kotlinx.datetime.LocalDate

public data class LifecycleEventFactors(
    val lengthOfDay: Duration,
    val temperature: Range<Temperature>,
    val date: LocalDate
)
