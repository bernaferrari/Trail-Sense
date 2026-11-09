package com.kylecorry.trail_sense.tools.weather.domain

import kotlin.time.Clock

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import kotlin.time.Duration
import kotlin.time.Instant

fun WeatherForecast.get3hTendency(): PressureTendency {
    return (tendency ?: PressureTendency.zero).let {
        it.copy(amount = it.amount * 3)
    }
}

fun List<Reading<CloudGenus?>>.getLastCloud(
    maxTime: Duration,
    now: Instant = Clock.System.now()
): Reading<CloudGenus?>? {
    val lastCloud = lastOrNull() ?: return null
    if ((now - lastCloud.time).absoluteValue > maxTime) {
        return null
    }
    return lastCloud
}
