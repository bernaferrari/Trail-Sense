package com.kylecorry.sol.science.astronomy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.fail
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.science.astronomy.eclipse.EclipseType
import com.kylecorry.sol.science.astronomy.moon.MoonPhaseTimeCalculator
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.stars.CONSTELLATIONS
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.tests.assertDate
import com.kylecorry.sol.tests.assertDuration
import com.kylecorry.sol.tests.parametrized
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.tests.ZonedTime
import com.kylecorry.sol.tests.atZone
import com.kylecorry.sol.tests.zone
import com.kylecorry.sol.tests.zoned
import com.kylecorry.sol.time.Time
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.math.min

class AstronomyTest {

    // TODO: Verify sun events other than actual time

    @Test
    fun isSuperMoonTrue() {
        isSuperMoonTrue("2021-04-26T12:00:00Z", true)
        // Not full
        isSuperMoonTrue("2021-04-21T12:00:00Z", false)
        // Not close enough
        isSuperMoonTrue("2021-09-21T12:00:00Z", false)
    }

    private fun isSuperMoonTrue(time: String, expected: Boolean) {
        val date = zoned(time)
        val actual = Astronomy.isSuperMoon(date.instant)
        assertEquals(expected, actual)
    }

    @Test
    fun canGetMoonDistance() {
        canGetMoonDistance("1992-04-12T00:00:00Z", 368409f)
    }

    private fun canGetMoonDistance(time: String, expected: Float) {
        val date = zoned(time)
        val position = Astronomy.getMoonPosition(date.instant, Coordinate.zero)
        assertEquals(expected, position.distance!!.value, 1f)
    }

    @Test
    fun canGetSunAngularDiameter() {
        val position = Astronomy.getSunPosition(
            zoned("2015-02-15T00:00:00Z").instant,
            Coordinate(0.0, 0.0)
        )

        assertEquals(0.539790f, position.angularDiameter!!, 0.0001f)
    }

    @Test
    fun canGetSunDistance() {
        val date = zoned("2015-02-15T00:00:00Z")
        val position = Astronomy.getSunPosition(date.instant, Coordinate.zero)

        assertEquals(147740816f, position.distance!!.value, 1f)
    }

    @Test
    fun canGetMoonAngularDiameter() {
        val position = Astronomy.getMoonPosition(
            zoned("2015-02-15T00:00:00Z").instant,
            Coordinate.zero
        )

        assertEquals(0.529422f, position.angularDiameter!!, 0.0001f)
    }

    @Test
    fun canGetNextPartialLunarEclipse() {
        providePartialLunarEclipses().forEach { args -> canGetNextPartialLunarEclipse(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<String>(), args[3].arg<String?>(), args[4].arg<String?>(), args[5].arg<String?>(), args[6].arg<Float>(), args[7].arg<Float>()) }
    }

    private fun canGetNextPartialLunarEclipse(
        latitude: Double,
        longitude: Double,
        date: String,
        start: String?,
        maximum: String?,
        end: String?,
        magnitude: Float,
        obscuration: Float
    ) {
        verifyEclipse(
            EclipseType.PartialLunar,
            latitude,
            longitude,
            date,
            start,
            maximum,
            end,
            magnitude,
            obscuration
        )
    }

    @Test
    fun canGetNextTotalEclipse() {
        provideTotalLunarEclipses().forEach { args -> canGetNextTotalEclipse(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<String>(), args[3].arg<String?>(), args[4].arg<String?>(), args[5].arg<String?>(), args[6].arg<Float>(), args[7].arg<Float>()) }
    }

    private fun canGetNextTotalEclipse(
        latitude: Double,
        longitude: Double,
        date: String,
        start: String?,
        maximum: String?,
        end: String?,
        magnitude: Float,
        obscuration: Float
    ) {
        verifyEclipse(
            EclipseType.TotalLunar,
            latitude,
            longitude,
            date,
            start,
            maximum,
            end,
            magnitude,
            obscuration
        )
    }

    @Test
    fun canGetNextSolarEclipse() {
        provideNextSolarEclipse().forEach { args -> canGetNextSolarEclipse(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<String>(), args[3].arg<String?>(), args[4].arg<String?>(), args[5].arg<String?>(), args[6].arg<Float>(), args[7].arg<Float>()) }
    }

    private fun canGetNextSolarEclipse(
        latitude: Double,
        longitude: Double,
        date: String,
        start: String?,
        maximum: String?,
        end: String?,
        magnitude: Float,
        obscuration: Float
    ) {
        verifyEclipse(
            EclipseType.Solar,
            latitude,
            longitude,
            date,
            start,
            maximum,
            end,
            magnitude,
            obscuration
        )
    }

    @Test
    fun getSunEventsActual() {
        getSunEventsActual(40.7128, -74.0060, "2020-09-12T12:00:00-04", "2020-09-12T06:34:00-04", "2020-09-12T12:52:00-04", "2020-09-12T19:09:00-04")
        getSunEventsActual(40.7128, -74.0060, "2020-09-22T12:00:00-04", "2020-09-22T06:44:00-04", "2020-09-22T12:48:00-04", "2020-09-22T18:52:00-04")
        getSunEventsActual(40.7128, -74.0060, "2020-03-21T12:00:00-04", "2020-03-21T06:57:00-04", "2020-03-21T13:02:00-04", "2020-03-21T19:10:00-04")
        getSunEventsActual(40.7128, -74.0060, "2020-12-21T12:00:00-05", "2020-12-21T07:15:00-05", "2020-12-21T11:54:00-05", "2020-12-21T16:32:00-05")
        getSunEventsActual(40.7128, -74.0060, "2020-06-21T12:00:00-04", "2020-06-21T05:25:00-04", "2020-06-21T12:58:00-04", "2020-06-21T20:31:00-04")
        getSunEventsActual(76.7667, -18.6667, "2020-06-04T12:00:00Z", null, "2020-06-04T13:13:00Z", null)
        getSunEventsActual(76.7667, -18.6667, "2020-11-01T12:00:00Z", null, null, null)
        getSunEventsActual(76.7667, -18.6667, "2020-10-30T12:00:00Z", "2020-10-30T12:05:00Z", "2020-10-30T12:58:00Z", "2020-10-30T13:49:00Z")
        getSunEventsActual(51.5, -0.13, "2020-10-29T12:00:00+01", "2020-10-29T07:48:00+01", "2020-10-29T12:44:00+01", "2020-10-29T17:39:00+01")
        getSunEventsActual(51.5, -0.13, "2020-10-30T12:00:00Z", "2020-10-30T06:50:00Z", "2020-10-30T11:44:00Z", "2020-10-30T16:37:00Z")
        getSunEventsActual(50.087778, 14.420556, "2022-12-27T12:00:00+01", "2022-12-27T08:00:00+01", "2022-12-27T12:03:00+01", "2022-12-27T16:08:00+01")
        getSunEventsActual(57.163, 65.549, "2023-04-25T12:00:00+05", "2023-04-25T05:02:00+05", "2023-04-25T12:35:00+05", "2023-04-25T20:10:00+05")
        getSunEventsActual(57.163, 65.549, "2023-04-26T12:00:00+05", "2023-04-26T05:02:00+05", "2023-04-26T12:35:00+05", "2023-04-26T20:12:00+05")
    }

    private fun getSunEventsActual(
        latitude: Double,
        longitude: Double,
        date: String,
        rise: String?,
        transit: String?,
        set: String?
    ) {
        val coordinate = Coordinate(latitude, longitude)
        val lookupDate = zoned(date)

        val sunEvents = Astronomy.getSunEvents(lookupDate.instant, coordinate, timeZone = lookupDate.zone, SunTimesMode.Actual, true)

        if (rise != null) {
            assertDate(
                zoned(rise),
                sunEvents.rise,
                2.minutes
            )
        } else {
            assertNull(sunEvents.rise)
        }

        if (transit != null) {
            assertDate(
                zoned(transit),
                sunEvents.transit,
                2.minutes
            )
        } else {
            assertNull(sunEvents.transit)
        }

        if (set != null) {
            assertDate(
                zoned(set),
                sunEvents.set,
                2.minutes
            )
        } else {
            assertNull(sunEvents.set)
        }
    }

    @Test
    fun getSunAltitude() {
        getSunAltitude(40.7128, -74.0060, "2020-09-13T09:08:00-04", 27.63f)
        getSunAltitude(40.7128, -74.0060, "2020-09-13T21:58:00-04", -30.863646f)
        getSunAltitude(40.7128, -74.0060, "2020-09-22T06:51:00-04", 0.9f)
    }

    private fun getSunAltitude(latitude: Double, longitude: Double, time: String, altitude: Float) {
        val coordinate = Coordinate(latitude, longitude)
        val datetime = zoned(time)

        val actual = Astronomy.getSunPosition(datetime.instant, coordinate, true)

        assertEquals(altitude, actual.altitude, 0.05f)
    }

    @Test
    fun getSunAzimuth() {
        getSunAzimuth(40.7128, -74.006, "2020-09-13T09:08:00-04", 111.0f)
        getSunAzimuth(40.7128, -74.006, "2020-09-13T21:58:00-04", 307.0f)
        getSunAzimuth(-21.48575, -55.54686, "2023-03-17T13:45:00-04", 302.18f)
    }

    private fun getSunAzimuth(latitude: Double, longitude: Double, date: String, expected: Float) {
        val coordinate = Coordinate(latitude, longitude)
        val datetime = zoned(date)
        val position = Astronomy.getSunPosition(datetime.instant, coordinate)
        assertEquals(expected, position.azimuth.value, 0.5f)
    }

    @Test
    fun getNextSunset() {
        val ny = Coordinate(40.7128, -74.0060)
        val gl = Coordinate(76.7667, -18.6667)
        val ri = Coordinate(41.915, -71.671)
        parametrized(
            listOf(
                listOf(
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 9, 0),
                    ny,
                    "America/New_York",
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 19, 7)
                ),
                listOf(
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 20, 0),
                    ny,
                    "America/New_York",
                    LocalDateTime(2020, Month.SEPTEMBER, 14, 19, 5)
                ),
                listOf(
                    LocalDateTime(2020, Month.JUNE, 4, 12, 0),
                    gl,
                    "America/Danmarkshavn",
                    null
                ),
                listOf(
                    LocalDateTime(2020, Month.AUGUST, 19, 12, 0),
                    gl,
                    "America/Danmarkshavn",
                    LocalDateTime(2020, Month.AUGUST, 20, 0, 56)
                ),
                listOf(
                    LocalDateTime(2023, Month.AUGUST, 6, 12, 0),
                    ri,
                    "America/New_York",
                    LocalDateTime(2023, Month.AUGUST, 6, 19, 59)
                )
            )
        ) {
            val zone = zone(it[2] as String)
            val time = (it[0] as LocalDateTime).atZone(zone)
            val sunset = Astronomy.getNextSunset(time.instant, it[1] as Coordinate, zone)
            val expected = (it[3] as LocalDateTime?)?.atZone(zone)
            assertDate(expected, sunset, 1.minutes)
        }
    }

    @Test
    fun getNextSunrise() {
        val ny = Coordinate(40.7128, -74.0060)
        val gl = Coordinate(76.7667, -18.6667)
        parametrized(
            listOf(
                listOf(
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 6, 0),
                    ny,
                    "America/New_York",
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 6, 35)
                ),
                listOf(
                    LocalDateTime(2020, Month.SEPTEMBER, 13, 20, 0),
                    ny,
                    "America/New_York",
                    LocalDateTime(2020, Month.SEPTEMBER, 14, 6, 36)
                ),
                listOf(
                    LocalDateTime(2020, Month.JUNE, 4, 12, 0),
                    gl,
                    "America/Danmarkshavn",
                    null
                ),
                listOf(
                    LocalDateTime(2020, Month.AUGUST, 19, 12, 0),
                    gl,
                    "America/Danmarkshavn",
                    LocalDateTime(2020, Month.AUGUST, 20, 1, 41)
                )
            )
        ) {
            val zone = zone(it[2] as String)
            val time = (it[0] as LocalDateTime).atZone(zone)
            val sunrise = Astronomy.getNextSunrise(time.instant, it[1] as Coordinate, zone)
            val expected = (it[3] as LocalDateTime?)?.atZone(zone)
            assertDate(expected, sunrise, 1.minutes)
        }
    }

    @Test
    fun getMoonEvents() {
        val cases = listOf(
            RiseSetTransitTestInput(
                LocalDate(2020, Month.SEPTEMBER, 12),
                LocalTime(0, 46),
                LocalTime(8, 34),
                LocalTime(16, 21)
            ),
            RiseSetTransitTestInput(
                LocalDate(2020, Month.SEPTEMBER, 11),
                null,
                LocalTime(7, 39),
                LocalTime(15, 27)
            ),
            RiseSetTransitTestInput(
                LocalDate(2020, Month.SEPTEMBER, 24),
                LocalTime(15, 1),
                LocalTime(19, 38),
                null
            ),
            RiseSetTransitTestInput(
                LocalDate(2020, Month.SEPTEMBER, 11),
                null,
                LocalTime(7, 53),
                null,
                Coordinate(76.7667, -18.6667),
                "America/Danmarkshavn"
            ),
            RiseSetTransitTestInput(
                LocalDate(2021, Month.FEBRUARY, 28),
                LocalTime(19, 29),
                LocalTime(1, 1),
                LocalTime(7, 35)
            )
        )

        for (case in cases) {
            val time = case.date.atTime(10, 0).atZone(case.zone)
            val actual = Astronomy.getMoonEvents(time.instant, case.location, case.zone)
            assertRst(case.expected(), actual)
        }
    }

    @Test
    fun getMoonAltitude() {
        getMoonAltitude(40.7128, -74.0060, "2020-09-13T09:08:00-04", 72.0f)
        getMoonAltitude(40.7128, -74.0060, "2020-09-13T21:58:00-04", -28.0f)
    }

    private fun getMoonAltitude(latitude: Double, longitude: Double, time: String, altitude: Float) {
        val actual = Astronomy.getMoonPosition(
            zoned(time).instant,
            Coordinate(latitude, longitude),
            true
        )
        assertEquals(altitude, actual.altitude, 0.8f)
    }

    @Test
    fun getMoonAzimuth() {
        getMoonAzimuth(40.7128, -74.0060, "2020-09-13T09:08:00-04", 165.0f)
        getMoonAzimuth(40.7128, -74.0060, "2020-09-13T21:58:00-04", 360.0f)
    }

    private fun getMoonAzimuth(latitude: Double, longitude: Double, time: String, azimuth: Float) {
        val actual = Astronomy.getMoonPosition(
            zoned(time).instant,
            Coordinate(latitude, longitude)
        )
        assertEquals(azimuth, actual.azimuth.value, 2f)
    }

    @Test
    fun getNextMoonset() {
        getNextMoonset(40.7128, -74.0060, "2020-09-13T09:00:00-04", "2020-09-13T17:10:00-04")
        getNextMoonset(40.7128, -74.0060, "2020-09-13T20:00:00-04", "2020-09-14T17:53:00-04")
        getNextMoonset(76.7667, -18.6667, "2020-09-08T12:00:00-01", null)
    }

    private fun getNextMoonset(latitude: Double, longitude: Double, time: String, moonset: String?) {
        val actual = Astronomy.getNextMoonset(
            zoned(time).instant,
            Coordinate(latitude, longitude),
            timeZone = zoned(time).zone
        )
        if (moonset != null) {
            assertDate(zoned(moonset), actual, 1.minutes)
        } else {
            assertNull(actual)
        }
    }

    @Test
    fun getNextMoonrise() {
        getNextMoonrise(40.7128, -74.0060, "2020-09-13T00:00:00-04", "2020-09-13T01:45:00-04")
        getNextMoonrise(40.7128, -74.0060, "2020-09-13T02:00:00-04", "2020-09-14T02:51:00-04")
        getNextMoonrise(76.7667, -18.6667, "2020-09-08T12:00:00-01", null)
    }

    private fun getNextMoonrise(latitude: Double, longitude: Double, time: String, moonrise: String?) {
        val actual = Astronomy.getNextMoonrise(
            zoned(time).instant,
            Coordinate(latitude, longitude),
            timeZone = zoned(time).zone
        )
        if (moonrise != null) {
            assertDate(zoned(moonrise), actual, 1.minutes)
        } else {
            assertNull(actual)
        }
    }

    @Test
    fun getMoonPhase() {
        getMoonPhase("2020-03-02T14:58:00-05", MoonTruePhase.FirstQuarter, 50.0f, 100.0f, 8.2f)
        getMoonPhase("2020-03-09T13:48:00-05", MoonTruePhase.Full, 100.0f, 184.1f, 15.1f)
        getMoonPhase("2020-03-16T05:35:00-04", MoonTruePhase.ThirdQuarter, 50.0f, 265.8f, 21.8f)
        getMoonPhase("2020-03-24T05:29:00-04", MoonTruePhase.New, 0.0f, 0.0f, 0.0f)
        getMoonPhase("2020-03-29T12:00:00-04", MoonTruePhase.WaxingCrescent, 23.0f, 64.6f, 5.3f)
        getMoonPhase("2020-03-05T12:00:00-05", MoonTruePhase.WaxingGibbous, 79.0f, 135.3f, 11.1f)
        getMoonPhase("2020-03-13T12:00:00-04", MoonTruePhase.WaningGibbous, 79.0f, 231.6f, 19.0f)
        getMoonPhase("2020-03-18T12:00:00-04", MoonTruePhase.WaningCrescent, 28.0f, 292.6f, 24.0f)
    }

    private fun getMoonPhase(
        date: String,
        phase: MoonTruePhase,
        illumination: Float,
        angle: Float,
        ageDays: Float
    ) {
        val tolerance = 0.5f

        val actual = Astronomy.getMoonPhase(zoned(date).instant)
        assertMoonPhases(
            MoonPhase(phase, illumination, angle, ageDays),
            actual,
            tolerance
        )
    }

    @Test
    fun isSunUp() {
        isSunUp(40.7128, -74.0060, "2020-09-13T12:00:00-04", true)
        isSunUp(40.7128, -74.0060, "2020-09-13T23:00:00-04", false)
    }

    private fun isSunUp(latitude: Double, longitude: Double, time: String, isUp: Boolean) {
        val actual = Astronomy.isSunUp(
            zoned(time).instant,
            Coordinate(latitude, longitude)
        )

        assertEquals(isUp, actual)
    }

    @Test
    fun isMoonUp() {
        isMoonUp(40.7128, -74.0060, "2020-09-13T09:08:00-04", true)
        isMoonUp(40.7128, -74.0060, "2020-09-13T21:58:00-04", false)
    }

    private fun isMoonUp(latitude: Double, longitude: Double, time: String, isUp: Boolean) {
        val actual = Astronomy.isMoonUp(
            zoned(time).instant,
            Coordinate(latitude, longitude)
        )

        assertEquals(isUp, actual)
    }

    // Test data from Stellarium
    @Test
    fun getPlanetPosition() {
        getPlanetPosition(1, "2025-07-21T03:00:00-04", 18.989444f, -33.458333f, null, 0.62f)
        getPlanetPosition(2, "2025-07-21T03:00:00-04", 64.500528f, 3.46775f, -3.92f, 1.09f)
        getPlanetPosition(4, "2025-07-21T03:00:00-04", 335.791861f, -40.415833f, 1.56f, 2.06f)
        getPlanetPosition(5, "2025-07-21T03:00:00-04", 47.614472f, -8.95975f, -1.74f, 6.10f)
        getPlanetPosition(6, "2025-07-21T03:00:00-04", 140.054722f, 38.865f, 0.89f, 9.07f)
        getPlanetPosition(7, "2025-07-21T03:00:00-04", 77.407861f, 16.33375f, 5.78f, 20.03f)
        getPlanetPosition(8, "2025-07-21T03:00:00-04", 139.64175f, 39.828778f, 7.85f, 29.42f)
    }

    private fun getPlanetPosition(
        planet: Int,
        time: String,
        azimuth: Float,
        altitude: Float,
        visualMagnitude: Float?,
        distance: Float,
    ) {
        val actual = Astronomy.getPlanetPosition(
            Planet.entries.first { it.order == planet },
            zoned(time).instant,
            Coordinate(41.97640, -71.65340),
            withRefraction = true,
            withParallax = true
        )

        assertEquals(azimuth, actual.azimuth.value, 0.5f)
        assertEquals(altitude, actual.altitude, 0.4f)
        // TODO: The visual magnitudes in the celestial calculation book do not line up with the ones reported by Stellarium
        if (visualMagnitude != null) {
            assertEquals(visualMagnitude, actual.visualMagnitude!!, 0.75f)
        }
        assertEquals(distance, actual.distance!!.value / 149597871, 0.01f)
    }

    @Test
    fun getMeteorShower() {
        getMeteorShower(40.7128, -74.0060, "2023-01-04T00:00:00-05", MeteorShower.Quadrantids, "2023-01-04T05:00:00-05")
        getMeteorShower(40.7128, -74.0060, "2023-04-22T00:00:00-04", MeteorShower.Lyrids, "2023-04-22T05:00:00-04")
        getMeteorShower(40.7128, -74.0060, "2023-05-06T00:00:00-04", MeteorShower.EtaAquariids, "2023-05-06T04:00:00-04")
        getMeteorShower(40.7128, -74.0060, "2023-07-31T00:00:00-04", MeteorShower.DeltaAquariids, "2023-07-31T03:00:00-04")
        getMeteorShower(40.7128, -74.0060, "2023-08-13T00:00:00-04", MeteorShower.Perseids, "2023-08-13T04:00:00-04")
        getMeteorShower(40.7128, -74.0060, "2023-10-21T00:00:00-04", MeteorShower.Orionids, "2023-10-21T05:00:00-04")
        getMeteorShower(40.7128, -74.0060, "2023-11-18T00:00:00-05", MeteorShower.Leonids, "2023-11-18T06:00:00-05")
        getMeteorShower(40.7128, -74.0060, "2023-12-14T00:00:00-05", MeteorShower.Geminids, "2023-12-14T01:00:00-05")
        getMeteorShower(40.7128, -74.0060, "2023-12-22T00:00:00-05", MeteorShower.Ursids, "2023-12-22T05:00:00-05")
        getMeteorShower(40.7128, -74.0060, "2023-01-01T00:00:00-05", null, null)
    }

    private fun getMeteorShower(
        latitude: Double,
        longitude: Double,
        time: String,
        shower: MeteorShower?,
        peak: String?
    ) {
        val actual = Astronomy.getMeteorShower(
            Coordinate(latitude, longitude),
            zoned(time).instant,
            timeZone = zoned(time).zone
        )

        assertEquals(shower, actual?.shower)
        if (peak != null) {
            assertDate(zoned(peak), actual?.peak, 1.hours)
        } else {
            assertNull(actual)
        }
    }

    @Test
    fun getActiveMeteorShowers() {
        // No shower
        getActiveMeteorShowers(40.7128, -74.0060, "2023-06-01T00:00:00-05", null, null)
        // Quadrantids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-01-04T05:00:00-05", MeteorShower.Quadrantids, null)
        // 10 days before (still active)
        getActiveMeteorShowers(40.7128, -74.0060, "2022-12-26T05:00:00-05", MeteorShower.Ursids, MeteorShower.Quadrantids)
        // 11 days before (not active)
        getActiveMeteorShowers(40.7128, -74.0060, "2022-12-25T05:00:00-05", MeteorShower.Ursids, null)
        // 10 days after (still active)
        getActiveMeteorShowers(40.7128, -74.0060, "2023-01-14T05:00:00-05", MeteorShower.Quadrantids, null)
        // 11 days after (not active)
        getActiveMeteorShowers(40.7128, -74.0060, "2023-01-15T05:00:00-05", null, null)
        // Lyrids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-04-22T05:00:00-04", MeteorShower.Lyrids, MeteorShower.EtaAquariids)
        // Eta Aquariids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-05-06T04:00:00-04", MeteorShower.EtaAquariids, null)
        // Delta Aquariids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-07-31T03:00:00-04", MeteorShower.DeltaAquariids, MeteorShower.Perseids)
        // Perseids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-08-13T04:00:00-04", MeteorShower.Perseids, null)
        // Orionids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-10-21T05:00:00-04", MeteorShower.Orionids, null)
        // Leonids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-11-18T06:00:00-05", MeteorShower.Leonids, null)
        // Geminids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-12-14T01:00:00-05", MeteorShower.Geminids, null)
        // Ursids
        getActiveMeteorShowers(40.7128, -74.0060, "2023-12-22T05:00:00-05", MeteorShower.Geminids, MeteorShower.Ursids)
    }

    private fun getActiveMeteorShowers(
        latitude: Double,
        longitude: Double,
        time: String,
        shower1: MeteorShower?,
        shower2: MeteorShower?
    ) {
        val actual = Astronomy.getActiveMeteorShowers(Coordinate(latitude, longitude), zoned(time).instant, timeZone = zoned(time).zone)
        val expected = listOfNotNull(shower1, shower2)
        println(actual)
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i].shower)
        }
    }

    @Test
    fun getAstronomicalSeason() {
        // Northern
        getAstronomicalSeason(40.7128, -74.0060, "2021-01-01T23:59:00-05", Season.Winter)
        getAstronomicalSeason(40.7128, -74.0060, "2021-03-19T23:59:00-04", Season.Winter)
        getAstronomicalSeason(40.7128, -74.0060, "2021-03-20T23:59:00-04", Season.Spring)
        getAstronomicalSeason(40.7128, -74.0060, "2021-06-19T23:59:00-04", Season.Spring)
        getAstronomicalSeason(40.7128, -74.0060, "2021-06-20T23:59:00-04", Season.Summer)
        getAstronomicalSeason(40.7128, -74.0060, "2021-09-21T23:59:00-04", Season.Summer)
        getAstronomicalSeason(40.7128, -74.0060, "2021-09-22T23:59:00-04", Season.Fall)
        getAstronomicalSeason(40.7128, -74.0060, "2021-12-20T23:59:00-05", Season.Fall)
        getAstronomicalSeason(40.7128, -74.0060, "2021-12-21T23:59:00-05", Season.Winter)
        getAstronomicalSeason(40.7128, -74.0060, "2021-12-31T23:59:00-05", Season.Winter)
        // Southern
        getAstronomicalSeason(-40.7128, -74.0060, "2021-01-01T23:59:00-05", Season.Summer)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-03-19T23:59:00-04", Season.Summer)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-03-20T23:59:00-04", Season.Fall)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-06-19T23:59:00-04", Season.Fall)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-06-20T23:59:00-04", Season.Winter)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-09-21T23:59:00-04", Season.Winter)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-09-22T23:59:00-04", Season.Spring)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-12-20T23:59:00-05", Season.Spring)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-12-21T23:59:00-05", Season.Summer)
        getAstronomicalSeason(-40.7128, -74.0060, "2021-12-31T23:59:00-05", Season.Summer)
    }

    private fun getAstronomicalSeason(latitude: Double, longitude: Double, time: String, season: Season) {
        val actual = Astronomy.getSeason(
            Coordinate(latitude, longitude),
            zoned(time).instant
        )

        assertEquals(season, actual)
    }

    @Test
    fun defaultGetSunEvents() {
        val expected = RiseSetTransitTestInput(
            LocalDate(2020, Month.SEPTEMBER, 12),
            LocalTime(6, 34),
            LocalTime(12, 52),
            LocalTime(19, 9)
        )

        val time = expected.date.atTime(12, 0).atZone(expected.zone)
        val actual = Astronomy.getSunEvents(time.instant, expected.location, expected.zone)
        assertRst(expected.expected(), actual)
    }

    @Test
    fun getDaylightLength() {
        provideDayLengths().forEach { args -> getDaylightLength(args[0].arg<ZonedTime>(), args[1].arg<Coordinate>(), args[2].arg<Duration>()) }
    }

    private fun getDaylightLength(
        date: ZonedTime,
        location: Coordinate,
        expected: Duration
    ) {
        val length = Astronomy.getDaylightLength(date.instant, location, timeZone = date.zone)
        assertDuration(expected, length, 1.minutes)
    }

    @Test
    fun getSunAboveHorizonTimes() {
        getSunAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T12:00:00-04", "2020-09-12T06:34:00-04", "2020-09-12T19:09:00-04")
        // Sun is set, and it is too early, use previous day
        getSunAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T00:00:00-04", "2020-09-11T06:33:00-04", "2020-09-11T19:11:00-04")
        // Sun is set, and it is not too early, use current day
        getSunAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T02:00:00-04", "2020-09-12T06:34:00-04", "2020-09-12T19:09:00-04")
        // Sun is set, and it is not too late, use current day
        getSunAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T20:00:00-04", "2020-09-12T06:34:00-04", "2020-09-12T19:09:00-04")
        // Sun is up and does not set
        getSunAboveHorizonTimes(76.7667, -18.6667, "2020-06-04T12:00:00Z", "2020-06-04T00:00:00Z", "2020-06-04T23:59:59Z")
        // Sun never rises
        getSunAboveHorizonTimes(76.7667, -18.6667, "2020-11-01T12:00:00Z", null, null)
    }

    private fun getSunAboveHorizonTimes(
        latitude: Double,
        longitude: Double,
        timeString: String,
        expectedStartString: String?,
        expectedEndString: String?
    ) {
        val location = Coordinate(latitude, longitude)
        val time = zoned(timeString)
        val expectedStart = expectedStartString?.let { zoned(it) }
        val expectedEnd = expectedEndString?.let { zoned(it) }
        val times = Astronomy.getSunAboveHorizonTimes(location, time.instant, timeZone = time.zone)
        assertDate(expectedStart, times?.start, 1.minutes)
        assertDate(expectedEnd, times?.end, 1.minutes)
    }

    @Test
    fun getMoonAboveHorizonTimes() {
        getMoonAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T12:00:00-04", "2020-09-12T00:46:00-04", "2020-09-12T16:21:00-04")
        // Moon is set, and it is too early, use previous day
        getMoonAboveHorizonTimes(40.7128, -74.0060, "2020-09-11T17:00:00-04", "2020-09-10T23:55:00-04", "2020-09-11T15:27:00-04")
        // Moon is set, and it is not too early, use current day
        getMoonAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T00:00:00-04", "2020-09-12T00:46:00-04", "2020-09-12T16:21:00-04")
        // Moon is set, and it is not too late, use current day
        getMoonAboveHorizonTimes(40.7128, -74.0060, "2020-09-12T17:00:00-04", "2020-09-12T00:46:00-04", "2020-09-12T16:21:00-04")
        // Moon is up and does not set
        getMoonAboveHorizonTimes(76.7667, -18.6667, "2020-09-11T00:00:00Z", "2020-09-11T00:00:00Z", "2020-09-11T23:59:59Z")
        // Moon never rises
        getMoonAboveHorizonTimes(76.7667, -18.6667, "2020-09-24T12:00:00Z", null, null)
    }

    private fun getMoonAboveHorizonTimes(
        latitude: Double,
        longitude: Double,
        timeString: String,
        expectedStartString: String?,
        expectedEndString: String?
    ) {
        val location = Coordinate(latitude, longitude)
        val time = zoned(timeString)
        val expectedStart = expectedStartString?.let { zoned(it) }
        val expectedEnd = expectedEndString?.let { zoned(it) }
        val times = Astronomy.getMoonAboveHorizonTimes(location, time.instant, timeZone = time.zone)
        assertDate(expectedStart, times?.start, 1.minutes)
        assertDate(expectedEnd, times?.end, 1.minutes)
    }

    @Test
    fun getMoonZenithAngleOfBrightLimb() {
        getMoonZenithAngleOfBrightLimb(0.0, 0.0, "2020-09-13T12:00:00Z", -3.2f)
        getMoonZenithAngleOfBrightLimb(42.0, -72.0, "2024-10-25T00:00:00Z", 113.4f)
        getMoonZenithAngleOfBrightLimb(-60.0, 180.0, "2024-10-25T00:00:00Z", -42.9f)
    }

    private fun getMoonZenithAngleOfBrightLimb(
        latitude: Double,
        longitude: Double,
        time: String,
        expected: Float
    ) {
        val actual = Astronomy.getMoonZenithAngleOfBrightLimb(
            zoned(time).instant,
            Coordinate(latitude, longitude)
        )
        assertEquals(expected, actual, 0.1f)
    }

    @Test
    fun getMoonPositionAngleOfBrightLimb() {
        getMoonPositionAngleOfBrightLimb("2020-09-13T12:00:00Z", 103.8f)
        getMoonPositionAngleOfBrightLimb("2024-10-25T00:00:00Z", 106.5f)
        getMoonPositionAngleOfBrightLimb("2026-01-01T00:00:00Z", -92.6f)
    }

    private fun getMoonPositionAngleOfBrightLimb(time: String, expected: Float) {
        val actual = Astronomy.getMoonPositionAngleOfBrightLimb(zoned(time).instant)

        assertEquals(expected, actual, 0.1f)
    }

    @Test
    fun getMoonParallacticAngle() {
        getMoonParallacticAngle(0.0, 0.0, "2020-09-13T12:00:00Z", 107.0f)
        getMoonParallacticAngle(42.0, -72.0, "2024-10-25T00:00:00Z", -6.9f)
        getMoonParallacticAngle(-60.0, 180.0, "2024-10-25T00:00:00Z", 149.4f)
    }

    private fun getMoonParallacticAngle(
        latitude: Double,
        longitude: Double,
        time: String,
        expected: Float
    ) {
        val actual = Astronomy.getMoonParallacticAngle(
            zoned(time).instant,
            Coordinate(latitude, longitude)
        )
        assertEquals(expected, actual, 0.1f)
    }

    @Test
    fun getLunarEclipseShadowPosition() {
        getLunarEclipseShadowPosition("2031-10-30T07:45:39Z", 251.38295f, 39.244385f, 1.37678f, 2.4432278f)
        getLunarEclipseShadowPosition("2035-08-19T01:10:35Z", 124.23559f, 15.728419f, 1.3635244f, 2.4342468f)
        getLunarEclipseShadowPosition("2026-08-28T04:12:34Z", 169.2296f, 37.858307f, 1.378456f, 2.4430807f)
    }

    private fun getLunarEclipseShadowPosition(
        timeString: String,
        expectedShadowAzimuth: Float,
        expectedShadowAltitude: Float,
        expectedUmbraDiameter: Float,
        expectedPenumbraDiameter: Float
    ) {
        val location = Coordinate(41.8240, -71.4128)
        val time = zoned(timeString)
        val shadow = Astronomy.getLunarEclipseShadow(time.instant, location, timeZone = time.zone)

        assertEquals(expectedShadowAzimuth, shadow.umbra.azimuth.value, 0.0001f)
        assertEquals(expectedShadowAltitude, shadow.umbra.altitude, 0.0001f)
        assertEquals(expectedShadowAzimuth, shadow.penumbra.azimuth.value, 0.0001f)
        assertEquals(expectedShadowAltitude, shadow.penumbra.altitude, 0.0001f)
        assertEquals(expectedUmbraDiameter, checkNotNull(shadow.umbra.angularDiameter), 0.0001f)
        assertEquals(
            expectedPenumbraDiameter,
            checkNotNull(shadow.penumbra.angularDiameter),
            0.0001f
        )
    }

    @Test
    fun getLunarEclipseShadowPositionWithRefractionAndParallax() {
        getLunarEclipseShadowPositionWithRefractionAndParallax("2031-10-30T07:45:39Z", 251.37576f, 38.514446f, 1.402901f, 2.489582f)
        getLunarEclipseShadowPositionWithRefractionAndParallax("2035-08-19T01:10:35Z", 124.22613f, 14.905734f, 1.3494445f, 2.4091105f)
        getLunarEclipseShadowPositionWithRefractionAndParallax("2026-08-28T04:12:34Z", 169.22636f, 37.149998f, 1.3822962f, 2.449887f)
    }

    private fun getLunarEclipseShadowPositionWithRefractionAndParallax(
        timeString: String,
        expectedShadowAzimuth: Float,
        expectedShadowAltitude: Float,
        expectedUmbraDiameter: Float,
        expectedPenumbraDiameter: Float
    ) {
        val location = Coordinate(41.8240, -71.4128)
        val time = zoned(timeString)
        val shadow = Astronomy.getLunarEclipseShadow(
            time.instant,
            location,
            timeZone = time.zone,
            withRefraction = true,
            withParallax = true
        )

        assertEquals(expectedShadowAzimuth, shadow.umbra.azimuth.value, 0.0001f)
        assertEquals(expectedShadowAltitude, shadow.umbra.altitude, 0.0001f)
        assertEquals(expectedShadowAzimuth, shadow.penumbra.azimuth.value, 0.0001f)
        assertEquals(expectedShadowAltitude, shadow.penumbra.altitude, 0.0001f)
        assertEquals(expectedUmbraDiameter, checkNotNull(shadow.umbra.angularDiameter), 0.0001f)
        assertEquals(
            expectedPenumbraDiameter,
            checkNotNull(shadow.penumbra.angularDiameter),
            0.0001f
        )
    }

//    @Test
//    fun solarEclipsePerformance() {
//        val start = zoned(2020, 1, 1)
//        performanceTest(1000) {
//            Astronomy.getNextEclipse(start.instant, Coordinate(0.0, 0.0), EclipseType.Solar, 365.days, timeZone = start.zone)
//        }
//    }

//    @Test
//    fun lunarEclipsePerformance() {
//        val start = zoned(2020, 1, 1)
//        performanceTest(1000) {
//            Astronomy.getNextEclipse(start.instant, Coordinate(0.0, 0.0), EclipseType.PartialLunar, 365.days, timeZone = start.zone)
//        }
//    }

    @Test
    fun canGetStarPosition() {
        canGetStarPosition("Sirius", 40.59117, -74.11905, "2024-11-30T23:00:00-05", 134.0935f, 19.0154f)
        canGetStarPosition("Betelgeuse", 40.59117, -74.11905, "2024-11-30T23:00:00-05", 129.6620f, 45.8404f)
        canGetStarPosition("Rigel", 40.59117, -74.11905, "2024-11-30T23:00:00-05", 151.6243f, 37.0076f)
    }

    private fun canGetStarPosition(
        name: String,
        latitude: Double,
        longitude: Double,
        time: String,
        azimuth: Float,
        altitude: Float
    ) {
        val star = STAR_CATALOG.find { it.name == name } ?: fail("Star not found")
        val actual = Astronomy.getStarPosition(
            star,
            zoned(time).instant,
            Coordinate(latitude, longitude),
            true
        )
        assertEquals(azimuth, actual.azimuth.value, 0.01f)
        assertEquals(altitude, actual.altitude, 0.01f)
    }

    @Test
    fun canGetZenithDistance() {
        canGetZenithDistance(68.06f, 1316.4f)
        canGetZenithDistance(67.92f, 1324.8f)
    }

    private fun canGetZenithDistance(altitude: Float, expected: Float) {
        val actual = Astronomy.getZenithDistance(altitude).convertTo(DistanceUnits.NauticalMiles).value
        assertEquals(expected, actual, 0.1f)
    }

    @Test
    fun canGetLocationFromStarsAltitudeOnly() {
        canGetLocationFromStarsAltitudeOnly(0, false)
        canGetLocationFromStarsAltitudeOnly(1, false)
        canGetLocationFromStarsAltitudeOnly(2, true)
        canGetLocationFromStarsAltitudeOnly(3, true)
        canGetLocationFromStarsAltitudeOnly(4, true)
        canGetLocationFromStarsAltitudeOnly(5, true)
        canGetLocationFromStarsAltitudeOnly(8, true)
        canGetLocationFromStarsAltitudeOnly(16, true)
    }

    private fun canGetLocationFromStarsAltitudeOnly(numberOfStars: Int, hasFix: Boolean) {
        val location = Coordinate(42.0, -72.0)
        val time = zoned(2024, 12, 3, zone = zone("America/New_York"))
        val stars = STAR_CATALOG.asSequence().filter { it.magnitude < 2.5 }.map { star ->
            val altitude = Astronomy.getStarPosition(star, time.instant, location, true).altitude
            val distance = Astronomy.getZenithDistance(altitude)
            star to distance
        }.sortedBy { it.second.value }
            .map { it.first }
            .take(numberOfStars).toList()

        val readings = stars.map {
            val position = Astronomy.getStarPosition(it, time.instant, location, true)
            StarReading(it, position.altitude, null, time.instant, time.zone)
        }

        val actual = Astronomy.getLocationFromStars(readings, Coordinate(40.0, -70.0))

        if (hasFix) {
            assertNotNull(actual)
            assertEquals(0f, actual!!.distanceTo(location), Distance.meters(20f).value)
        } else {
            assertNull(actual)
        }
    }

    @Test
    fun canGetLocationFromStarsAltitudeAndAzimuth() {
        canGetLocationFromStarsAltitudeAndAzimuth(0, false)
        canGetLocationFromStarsAltitudeAndAzimuth(1, false)
        canGetLocationFromStarsAltitudeAndAzimuth(2, true)
        canGetLocationFromStarsAltitudeAndAzimuth(3, true)
        canGetLocationFromStarsAltitudeAndAzimuth(4, true)
        canGetLocationFromStarsAltitudeAndAzimuth(5, true)
        canGetLocationFromStarsAltitudeAndAzimuth(8, true)
        canGetLocationFromStarsAltitudeAndAzimuth(16, true)
    }

    private fun canGetLocationFromStarsAltitudeAndAzimuth(numberOfStars: Int, hasFix: Boolean) {
        val location = Coordinate(42.0, -72.0)
        val time = zoned(2024, 12, 3, zone = zone("America/New_York"))
        val altitudeBias = if (numberOfStars > 2) 1f else 0f
        val azimuthBias = if (numberOfStars > 2) 3f else 0f
        val stars = STAR_CATALOG.asSequence().filter { it.magnitude < 2.5 }.map { star ->
            val altitude = Astronomy.getStarPosition(star, time.instant, location, true).altitude
            val distance = Astronomy.getZenithDistance(altitude)
            star to distance
        }.sortedBy { it.second.value }
            .map { it.first }
            .take(numberOfStars).toList()

        val readings = stars.map {
            val position = Astronomy.getStarPosition(it, time.instant, location, true)
            val altitude = position.altitude + altitudeBias
            val azimuth = position.azimuth.value + azimuthBias
            StarReading(it, altitude, azimuth, time.instant, time.zone)
        }

        val actual = Astronomy.getLocationFromStars(readings)

        if (hasFix) {
            assertNotNull(actual)
            assertEquals(0f, actual!!.distanceTo(location), Distance.meters(10f).value)
        } else {
            assertNull(actual)
        }
    }

    @Test
    fun plateSolve() {
        val time = zoned(2024, 12, 1)
        val location = Coordinate(42.0, -72.0)
        val actualStars = CONSTELLATIONS.first { it.name == "Orion" }.starEdges.flatMap { listOf(it.first, it.second) }.distinct()
        val readings = actualStars.map {
            val position = Astronomy.getStarPosition(it, time.instant, location, true)
            CelestialObservation(position.azimuth, position.altitude)
        }

        val stars = Astronomy.plateSolve(readings, time.instant, timeZone = time.zone).map { it.star }

        // Verify that the solver found the correct stars
        // TODO: This isn't doing a good job when there are more stars in the catalog
//        assertEquals(actualStars.size, stars.size)
        assertTrue(actualStars.containsAll(stars))
    }

    private fun assertMoonPhases(expected: MoonPhase, actual: MoonPhase, tolerance: Float) {
        assertEquals(expected.phase, actual.phase)
        assertEquals(expected.illumination, actual.illumination, tolerance)
        val angleDifference = (expected.phaseAngle - actual.phaseAngle).absoluteValue
        val circularAngleDifference = min(angleDifference, 360f - angleDifference)
        val oneDayInDegrees = 360f / MoonPhaseTimeCalculator.SYNODIC_MONTH_DAYS.toFloat()
        assertEquals(0f, circularAngleDifference, oneDayInDegrees)
        val ageDifference = (expected.lunarAge - actual.lunarAge).absoluteValue
        val circularAgeDifference = min(
            ageDifference,
            MoonPhaseTimeCalculator.SYNODIC_MONTH_DAYS.toFloat() - ageDifference
        )
        assertEquals(0f, circularAgeDifference, 0.05f)
    }

    private fun assertRst(
        expected: RiseSetTransitTimes,
        actual: RiseSetTransitTimes,
        maxDifference: Duration = 1.minutes
    ) {
        assertDate(expected.rise, actual.rise, maxDifference)
        assertDate(expected.transit, actual.transit, maxDifference)
        assertDate(expected.set, actual.set, maxDifference)
    }

    data class RiseSetTransitTestInput(
        val date: LocalDate,
        val rise: LocalTime?,
        val transit: LocalTime?,
        val set: LocalTime?,
        val location: Coordinate = Coordinate(40.7128, -74.0060),
        val zone: TimeZone = zone("America/New_York")
    ) {
        constructor(
            date: LocalDate,
            rise: LocalTime?,
            transit: LocalTime?,
            set: LocalTime?,
            location: Coordinate,
            zone: String
        ) : this(date, rise, transit, set, location, zone(zone))

        fun expected() = RiseSetTransitTimes(
            rise?.let { date.atTime(it).atZone(zone).instant },
            transit?.let { date.atTime(it).atZone(zone).instant },
            set?.let { date.atTime(it).atZone(zone).instant }
        )
    }

    private fun verifyEclipse(
        type: EclipseType,
        latitude: Double,
        longitude: Double,
        date: String,
        start: String?,
        maximum: String?,
        end: String?,
        magnitude: Float,
        obscuration: Float
    ) {
        val datetime = zoned(date)
        val location = Coordinate(latitude, longitude)

        val actual = Astronomy.getNextEclipse(datetime.instant, location, type, timeZone = datetime.zone)

        if (start == null || end == null) {
            assertNull(actual)
            return
        }

        assertNotNull(actual)

        assertDate(zoned(start), actual!!.start, 2.minutes)
        assertDate(zoned(checkNotNull(maximum)), actual.maximum, 2.minutes)
        assertDate(zoned(end), actual.end, 2.minutes)

        assertEquals(magnitude, actual.magnitude, 0.005f)
        assertEquals(obscuration, actual.obscuration, 0.005f)
    }

    companion object {
        fun provideTotalLunarEclipses(): List<List<Any?>> {
            return listOf(
                listOf(
                    42.0,
                    -70.0,
                    "2021-08-29T00:00:00Z",
                    "2022-05-16T03:29:00Z",
                    "2022-05-16T04:11:00Z",
                    "2022-05-16T04:53:00Z",
                    1.414f,
                    1f
                )
            )
        }
        fun providePartialLunarEclipses(): List<List<Any?>> {
            return listOf(
                listOf(
                    42.0,
                    -70.0,
                    "2021-08-29T00:00:00Z",
                    "2021-11-19T07:18:00Z",
                    "2021-11-19T09:03:00Z",
                    "2021-11-19T10:47:00Z",
                    0.974f,
                    0.991f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2021-12-01T10:47:00Z",
                    "2022-05-16T02:28:00Z",
                    "2022-05-16T04:11:00Z",
                    "2022-05-16T05:54:00Z",
                    1.413f,
                    1f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2023-01-01T00:00:00Z",
                    "2024-09-18T02:13:00Z",
                    "2024-09-18T02:44:00Z",
                    "2024-09-18T03:15:00Z",
                    0.078f,
                    0.035f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2024-09-19T00:00:00Z",
                    "2025-03-14T05:09:00Z",
                    "2025-03-14T06:58:00Z",
                    "2025-03-14T08:47:00Z",
                    1.178f,
                    1f
                ),
                listOf(
                    42.0,
                    70.0,
                    "2021-12-01T10:47:00Z",
                    "2022-11-08T12:04:00Z",
                    "2022-11-08T12:04:00Z",
                    "2022-11-08T12:49:00Z",
                    1.359f,
                    1f
                ),

                // At start
                listOf(
                    42.0,
                    -70.0,
                    "2021-11-19T07:18:00Z",
                    "2021-11-19T07:18:00Z",
                    "2021-11-19T09:03:00Z",
                    "2021-11-19T10:47:00Z",
                    0.974f,
                    0.991f
                ),

                // At middle
                listOf(
                    42.0,
                    -70.0,
                    "2021-11-19T09:03:00Z",
                    "2021-11-19T07:18:00Z",
                    "2021-11-19T09:03:00Z",
                    "2021-11-19T10:47:00Z",
                    0.974f,
                    0.991f
                ),

                // At end
                listOf(
                    42.0,
                    -70.0,
                    "2021-11-19T10:47:00Z",
                    "2022-05-16T02:28:00Z",
                    "2022-05-16T04:11:00Z",
                    "2022-05-16T05:54:00Z",
                    1.413f,
                    1f
                ),

                // Trim to moonset
                listOf(
                    42.0,
                    -70.0,
                    "2026-03-01T00:00:00Z",
                    "2026-03-03T09:51:00Z",
                    "2026-03-03T11:11:00Z",
                    "2026-03-03T11:11:00Z",
                    1.148f,
                    1f
                ),
            )
        }
        fun provideNextSolarEclipse(): List<List<Any?>> {
            return listOf(
                listOf(
                    24.61167,
                    143.36167,
                    "2009-07-22T00:00:00Z",
                    "2009-07-22T00:58:00Z",
                    "2009-07-22T02:19:00Z",
                    "2009-07-22T03:42:00Z",
                    0.7456f,
                    0.69520104f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2023-01-01T00:00:00Z",
                    "2023-10-14T16:21:00Z",
                    "2023-10-14T17:28:00Z",
                    "2023-10-14T18:38:00Z",
                    0.2913301f,
                    0.17762049f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2023-10-15T00:00:00Z",
                    "2024-04-08T18:18:00Z",
                    "2024-04-08T19:30:00Z",
                    "2024-04-08T20:41:00Z",
                    0.92334f,
                    0.91496944f
                ),
                listOf(
                    42.0,
                    -70.0,
                    "2024-04-09T00:00:00Z",
                    "2025-03-29T10:29:00Z",
                    "2025-03-29T10:29:00Z",
                    "2025-03-29T11:08:00Z",
                    0.6743827f,
                    0.6000332f
                ),
                listOf(
                    25.28,
                    -104.12,
                    "2023-10-15T00:00:00Z",
                    "2024-04-08T17:03:00Z",
                    "2024-04-08T18:23:00Z",
                    "2024-04-08T19:46:00Z",
                    0.94278395f,
                    0.94278395f
                ), // This should be a total eclipse

                // No eclipses
                listOf(40.0, 120.0, "2023-01-01T00:00:00Z", null, null, null, 0.0f, 0.0f),
                listOf(40.0, 120.0, "2023-10-15T00:00:00Z", null, null, null, 0.0f, 0.0f),

                // Search starts at start of eclipse
                listOf(
                    42.0,
                    -70.0,
                    "2023-10-14T16:21:00Z",
                    "2023-10-14T16:21:00Z",
                    "2023-10-14T17:28:00Z",
                    "2023-10-14T18:38:00Z",
                    0.29133186f,
                    0.17762049f
                ),
                // Search starts during eclipse
                listOf(
                    42.0,
                    -70.0,
                    "2023-10-14T18:00:00Z",
                    "2023-10-14T16:21:00Z",
                    "2023-10-14T17:28:00Z",
                    "2023-10-14T18:38:00Z",
                    0.29133186f,
                    0.17762049f
                ),
                // Search starts at end of eclipse
                listOf(
                    42.0,
                    -70.0,
                    "2023-10-14T18:38:00Z",
                    "2024-04-08T18:18:00Z",
                    "2024-04-08T19:30:00Z",
                    "2024-04-08T20:41:00Z",
                    0.92334f,
                    0.91496944f
                ),
                // Edge case - findStart used to miss this
                listOf(
                    46.125,
                    -72.0,
                    "2024-04-07T15:27:00Z",
                    "2024-04-08T18:17:00Z",
                    "2024-04-08T19:28:00Z",
                    "2024-04-08T20:37:00Z",
                    1.0519722f,
                    1.0f
                )
            )
        }
        fun provideDayLengths(): List<List<Any?>> {
            val ny = Coordinate(40.7128, -74.0060)
            val zoneNy = zone("America/New_York")
            val greenland = Coordinate(76.7667, -18.6667)
            val zoneGreenland = zone("America/Danmarkshavn")
            return listOf(
                listOf(zoned(2021, 4, 3, zone = zoneNy), ny, Time.duration(12, 47)),
                listOf(zoned(2021, 3, 13, zone = zoneNy), ny, Time.duration(11, 50)),
                listOf(zoned(2021, 3, 14, zone = zoneNy), ny, Time.duration(11, 53)),
                listOf(zoned(2021, 1, 10, zone = zoneGreenland), greenland, Time.duration(0)),
                listOf(zoned(2021, 2, 17, zone = zoneGreenland), greenland, Time.duration(4, 38)),
                listOf(zoned(2021, 4, 22, zone = zoneGreenland), greenland, Time.duration(22, 42)),
                listOf(zoned(2021, 5, 22, zone = zoneGreenland), greenland, Time.duration(24))
            )
        }
    }
}
