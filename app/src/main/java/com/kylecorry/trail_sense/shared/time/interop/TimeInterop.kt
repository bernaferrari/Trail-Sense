package com.kylecorry.trail_sense.shared.time.interop

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import java.time.ZonedDateTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

/*
 * Boundary overloads between java.time (Android UI, Room entities, legacy preferences) and the
 * kotlin.time / kotlinx-datetime types used by sol and andromeda. Each one converts losslessly
 * and is only picked when the library member does not accept the java type.
 */

fun <T> Reading(value: T, time: java.time.Instant): Reading<T> = Reading(value, time.toKotlinInstant())

fun Preferences.putInstant(key: String, value: java.time.Instant) = putInstant(key, value.toKotlinInstant())
fun Preferences.putDuration(key: String, value: java.time.Duration) = putDuration(key, value.toKotlinDuration())
fun Preferences.putLocalDate(key: String, value: java.time.LocalDate) = putLocalDate(key, value.toKotlinLocalDate())
fun Preferences.getJavaLocalDate(key: String): java.time.LocalDate? = getLocalDate(key)?.toJavaLocalDate()
fun Preferences.getJavaInstant(key: String): java.time.Instant? = getInstant(key)?.toJavaInstant()
fun Preferences.getJavaDuration(key: String): java.time.Duration? =
    getDuration(key)?.let { java.time.Duration.ofMillis(it.inWholeMilliseconds) }

operator fun kotlin.time.Instant.plus(duration: java.time.Duration): kotlin.time.Instant = this + duration.toKotlinDuration()
operator fun kotlin.time.Instant.minus(duration: java.time.Duration): kotlin.time.Instant = this - duration.toKotlinDuration()
operator fun kotlin.time.Instant.compareTo(other: java.time.Instant): Int = compareTo(other.toKotlinInstant())
operator fun java.time.Instant.compareTo(other: kotlin.time.Instant): Int = compareTo(other.toJavaInstant())
fun kotlin.time.Instant.toEpochMilli(): Long = toEpochMilliseconds()

fun Astronomy.getMoonPhase(time: ZonedDateTime) = getMoonPhase(time.toInstant().toKotlinInstant())
fun Astronomy.getSolarRadiation(
    time: ZonedDateTime,
    location: Coordinate,
    tilt: Float,
    azimuth: Bearing,
    withRefraction: Boolean = false
) = getSolarRadiation(time.toInstant().toKotlinInstant(), location, tilt, azimuth, withRefraction)
