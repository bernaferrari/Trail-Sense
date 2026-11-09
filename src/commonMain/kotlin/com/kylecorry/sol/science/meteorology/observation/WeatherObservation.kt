package com.kylecorry.sol.science.meteorology.observation

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Speed
import kotlin.time.Instant

public sealed class WeatherObservation<T>(
    public val time: Instant,
    public val value: T
) {
    public class Pressure(time: Instant, value: com.kylecorry.sol.units.Pressure) :
        WeatherObservation<com.kylecorry.sol.units.Pressure>(time, value)

    public class WindSpeed(time: Instant, value: Speed) : WeatherObservation<Speed>(time, value)

    /**
     * A wind direction observation - the direction the wind is coming from.
     */
    public class WindDirection(time: Instant, value: Bearing) : WeatherObservation<Bearing>(time, value)
    public class CloudGenus(time: Instant, value: com.kylecorry.sol.science.meteorology.clouds.CloudGenus?) :
        WeatherObservation<com.kylecorry.sol.science.meteorology.clouds.CloudGenus?>(time, value)

    public fun asReading(): Reading<T> {
        return Reading(value, time)
    }

}
