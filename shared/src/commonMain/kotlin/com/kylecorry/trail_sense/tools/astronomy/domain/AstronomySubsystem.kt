package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.time.today
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class AstronomySubsystem private constructor() {

    private val astronomyService = AstronomyService()
    private val location = getAppService<ILocationSubsystem>()
    private val prefs = getAppService<UserPreferences>()
    private val hooks = Hooks()
    private val hookTriggers = HookTriggers()

    val sun: SunDetails
        get() = hooks.memo(
            "sun",
            prefs.astronomy.sunTimesMode,
            hookTriggers.distance("sun", location.location, Distance.kilometers(1f)),
            hookTriggers.frequency("sun", 1.minutes)
        ) {
            val sunTimesMode = prefs.astronomy.sunTimesMode
            val location = location.location
            val times = astronomyService.getSunTimes(location, sunTimesMode, today())
            val azimuth = astronomyService.getSunAzimuth(location)
            val altitude = astronomyService.getSunAltitude(location)

            val nextSunrise = astronomyService.getNextSunrise(location, sunTimesMode)
            val nextSunset = astronomyService.getNextSunset(location, sunTimesMode)

            SunDetails(
                times.rise,
                times.set,
                times.transit,
                altitude > 0,
                nextSunrise,
                nextSunset,
                altitude,
                azimuth
            )
        }

    val moon: MoonDetails
        get() = hooks.memo(
            "moon",
            hookTriggers.distance("moon", location.location, Distance.kilometers(1f)),
            hookTriggers.frequency("moon", 1.minutes)
        ) {
            val location = location.location
            val moonTimes = astronomyService.getMoonTimes(location, today())
            val phase = astronomyService.getCurrentMoonPhase()
            val altitude = astronomyService.getMoonAltitude(location)
            val azimuth = astronomyService.getMoonAzimuth(location)
            val tilt = astronomyService.getMoonTilt(location)

            MoonDetails(
                moonTimes.rise,
                moonTimes.set,
                moonTimes.transit,
                altitude > 0,
                phase.phase,
                phase.phaseAngle,
                phase.illumination,
                altitude,
                azimuth,
                tilt
            )
        }

    fun getSunAltitudes(): List<Reading<Float>> {
        return if (!prefs.astronomy.centerSunAndMoon) {
            astronomyService.getSunAltitudes(location.location, today())
        } else {
            astronomyService.getCenteredSunAltitudes(location.location, Clock.System.now())
        }
    }

    fun getMoonAltitudes(): List<Reading<Float>> {
        return if (!prefs.astronomy.centerSunAndMoon) {
            astronomyService.getMoonAltitudes(location.location, today())
        } else {
            astronomyService.getCenteredMoonAltitudes(location.location, Clock.System.now())
        }
    }

    fun isNight(): Boolean {
        return !sun.isUp
    }

    companion object {
        private val shared by lazy { AstronomySubsystem() }

        fun getInstance(): AstronomySubsystem = shared
    }

}

