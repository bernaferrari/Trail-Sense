package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.meteors.JvmMeteorShowerPeak
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Coordinate
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal object MeteorFacade {
    fun getMeteorShower(location: Coordinate, date: ZonedDateTime): JvmMeteorShowerPeak? =
        MeteorModel.shower(location, date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone())
            ?.toJvm(date)

    fun getActiveMeteorShowers(location: Coordinate, date: ZonedDateTime): List<JvmMeteorShowerPeak> =
        MeteorModel.active(location, date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone())
            .map { it.toJvm(date) }

    fun getMeteorShowerPosition(shower: MeteorShower, location: Coordinate, time: Instant): CelestialObservation =
        MeteorModel.position(shower, location, time.toKotlinInstant())

    private fun MeteorPeak.toJvm(date: ZonedDateTime) = JvmMeteorShowerPeak(
        shower, start.toJavaInstant().atZone(date.zone), peak.toJavaInstant().atZone(date.zone),
        end.toJavaInstant().atZone(date.zone)
    )
}
