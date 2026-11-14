package com.kylecorry.sol.science.meteorology

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import assertk.assertThat
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.tests.isCloseTo
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

class MeteorologyTest {

    @Test
    fun altitude() {
        provideAltitudes().forEach { args -> altitude(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun altitude(pressure: Float, seaLevel: Float, altitude: Float) {
        val actual = Meteorology.getAltitude(Pressure.hpa(pressure), Pressure.hpa(seaLevel))
        val expected = Distance.meters(altitude)
        assertThat(actual).isCloseTo(expected, 1f)
    }

    @Test
    fun forecast() {
        provideForecasts().forEach { args -> forecast(args[0].arg<PressureCharacteristic>(), args[1].arg<Float>(), args[2].arg<Float?>(), args[3].arg<Weather>()) }
    }

    private fun forecast(
        characteristic: PressureCharacteristic,
        tendencyAmount: Float,
        stormThresh: Float?,
        expectedWeather: Weather
    ) {
        val tendency = PressureTendency(characteristic, tendencyAmount)
        val weather = Meteorology.forecast(tendency, stormThresh)
        assertEquals(expectedWeather, weather)
    }

    @Test
    fun forecastFromReadings() {
        provideWeatherForecasts().forEach { args -> forecastFromReadings(args[0].arg<List<Reading<Pressure>>>(), args[1].arg<List<Reading<CloudGenus?>>>(), args[2].arg<Range<Temperature>?>(), args[3].arg<WeatherForecast>(), args[4].arg<WeatherForecast>()) }
    }

    private fun forecastFromReadings(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        temperatures: Range<Temperature>?,
        now: WeatherForecast,
        then: WeatherForecast
    ) {
        val weather = Meteorology.forecast(pressures, clouds, temperatures, time = weatherTime)
        assertEquals(now, weather.first())
        assertEquals(then, weather.last())
    }

    @Test
    fun tendency() {
        provideTendencies().forEach { args -> tendency(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Duration>(), args[3].arg<Float>(), args[4].arg<PressureTendency>()) }
    }

    private fun tendency(
        lastHpa: Float,
        currentHpa: Float,
        duration: Duration,
        threshold: Float,
        expectedTendency: PressureTendency
    ) {
        val tendency = Meteorology.getTendency(Pressure.hpa(lastHpa), Pressure.hpa(currentHpa), duration, threshold)
        assertEquals(expectedTendency, tendency)
    }

    @Test
    fun heatIndex() {
        provideHeatIndex().forEach { args -> heatIndex(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun heatIndex(temperature: Float, humidity: Float, expected: Float) {
        val hi = Meteorology.getHeatIndex(temperature, humidity)
        assertEquals(expected, hi, 0.5f)
    }

    @Test
    fun heatAlert() {
        provideHeatAlert().forEach { args -> heatAlert(args[0].arg<Float>(), args[1].arg<HeatAlert>()) }
    }

    private fun heatAlert(heatIndex: Float, expected: HeatAlert) {
        val alert = Meteorology.getHeatAlert(heatIndex)
        assertEquals(expected, alert)
    }

    @Test
    fun getWindChill() {
        getWindChill(40f, 5f, 36f)
        getWindChill(10f, 25f, -11f)
        getWindChill(-10f, 15f, -32f)
        getWindChill(-15f, 40f, -50f)
        getWindChill(-45f, 60f, -98f)
        getWindChill(50.5f, 10f, 50.5f)
        getWindChill(0f, 3f, 0f)
    }

    private fun getWindChill(temperature: Float, windSpeed: Float, expected: Float) {
        val chill = Meteorology.getWindChill(
            Temperature.fahrenheit(temperature),
            Speed.from(windSpeed, DistanceUnits.Miles, TimeUnits.Hours)
        )
        assertEquals(expected, chill.value, 0.5f)
    }

    @Test
    fun dewPoint() {
        provideDewPoint().forEach { args -> dewPoint(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun dewPoint(temperature: Float, humidity: Float, expected: Float) {
        val dew = Meteorology.getDewPoint(temperature, humidity)
        assertEquals(expected, dew, 0.5f)
    }

    @Test
    fun lightningStrikes() {
        provideLightningStrikes().forEach { args -> lightningStrikes(args[0].arg<Instant>(), args[1].arg<Instant>(), args[2].arg<Float>()) }
    }

    private fun lightningStrikes(lightning: Instant, thunder: Instant, expected: Float) {
        val distance = Meteorology.getLightningStrikeDistance(lightning, thunder)
        assertEquals(expected, distance, 0.5f)
    }

    @Test
    fun lightningStrikeDanger() {
        provideLightningStrikeDistances().forEach { args -> lightningStrikeDanger(args[0].arg<Float>(), args[1].arg<Boolean>()) }
    }

    private fun lightningStrikeDanger(distanceMeters: Float, expected: Boolean) {
        val danger = Meteorology.isLightningStrikeDangerous(Distance.meters(distanceMeters))
        assertEquals(expected, danger)
    }

    @Test
    fun seasons() {
        provideSeasons().forEach { args -> seasons(args[0].arg<Season>(), args[1].arg<Boolean>(), args[2].arg<LocalDate>()) }
    }

    private fun seasons(expected: Season, isNorth: Boolean, date: LocalDate) {
        val season = Meteorology.getSeason(
            Coordinate(if (isNorth) 1.0 else -1.0, 0.0),
            date
        )
        assertEquals(expected, season)
    }

    @Test
    fun temperatureAtElevation() {
        temperatureAtElevation(20.0f, 0.0f, 0.0f, 20.0f)
        temperatureAtElevation(20.0f, 0.0f, 1000.0f, 13.5f)
        temperatureAtElevation(0.0f, 1000.0f, 0.0f, 6.5f)
        temperatureAtElevation(-10.0f, 0.0f, 1000.0f, -16.5f)
        temperatureAtElevation(25.0f, 100.0f, 200.0f, 24.35f)
        temperatureAtElevation(15.0f, 0.0f, 5000.0f, -17.5f)
        temperatureAtElevation(0.0f, 0.0f, 50000.0f, -273.15f)
    }

    private fun temperatureAtElevation(
        temperatureCelsius: Float,
        baseElevationMeters: Float,
        destElevationMeters: Float,
        expectedCelsius: Float
    ) {
        val result = Meteorology.getTemperatureAtElevation(
            Temperature.celsius(temperatureCelsius),
            Distance.meters(baseElevationMeters),
            Distance.meters(destElevationMeters)
        )
        assertEquals(expectedCelsius, result.celsius().value, 0.1f)
    }

    @Test
    fun ambientTemperature() {
        val temp = Meteorology.getAmbientTemperature(170f, 125f, 100f)
        assertEquals(68.75f, temp)
    }

    @Test
    fun convertsToSeaLevel() {
        provideSeaLevelPressure().forEach { args -> convertsToSeaLevel(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float?>(), args[3].arg<Float>()) }
    }

    private fun convertsToSeaLevel(
        pressureHpa: Float,
        altitudeMeters: Float,
        temperatureCelsius: Float?,
        expectedPressureHpa: Float
    ) {
        val altitude = Distance.meters(altitudeMeters)
        val reading = Meteorology.getSeaLevelPressure(
            Pressure.hpa(pressureHpa),
            altitude,
            temperatureCelsius?.let { Temperature.celsius(it) })
        assertEquals(expectedPressureHpa, reading.hpa().value, 0.1f)
    }

    @Test
    fun isHigh() {
        provideHighPressures().forEach { args -> isHigh(args[0].arg<Float>(), args[1].arg<Boolean>()) }
    }

    private fun isHigh(pressureHpa: Float, isHigh: Boolean) {
        val ret = Meteorology.isHighPressure(Pressure.hpa(pressureHpa))
        assertEquals(isHigh, ret)
    }

    @Test
    fun isLow() {
        provideLowPressures().forEach { args -> isLow(args[0].arg<Float>(), args[1].arg<Boolean>()) }
    }

    private fun isLow(pressureHpa: Float, isLow: Boolean) {
        val ret = Meteorology.isLowPressure(Pressure.hpa(pressureHpa))
        assertEquals(isLow, ret)
    }

    @Test
    fun getKoppenGeigerClimateClassification() {
        val tests = listOf(
            //  Tropical
            Triple(
                "Af",
                monthlyTemperatures(26f, 26f, 26f, 26f, 26f, 25f, 25f, 26f, 26f, 26f, 26f, 26f),
                monthlyPrecipitation(
                    240.8f,
                    230f,
                    256.2f,
                    256.2f,
                    212f,
                    150.6f,
                    121.2f,
                    126.1f,
                    147.5f,
                    193.6f,
                    205.8f,
                    222.6f
                )
            ),
            Triple(
                "Am",
                monthlyTemperatures(27f, 27f, 27f, 27f, 27f, 27f, 27f, 28f, 28f, 28f, 28f, 28f),
                monthlyPrecipitation(
                    228.5f,
                    281.8f,
                    279.5f,
                    240.6f,
                    153.9f,
                    73.6f,
                    45.8f,
                    48f,
                    67.6f,
                    97.9f,
                    122.6f,
                    167.8f
                )
            ),
            Triple(
                "As",
                monthlyTemperatures(22f, 23f, 22f, 22f, 21f, 19f, 19f, 21f, 23f, 23f, 22f, 22f),
                monthlyPrecipitation(
                    203.2f,
                    178.9f,
                    168.5f,
                    82.3f,
                    20.2f,
                    6.2f,
                    3.1f,
                    9.8f,
                    34.6f,
                    103.3f,
                    185.6f,
                    220f
                )
            ),

            // Dry
            Triple(
                "BWh",
                monthlyTemperatures(12f, 15f, 20f, 25f, 30f, 32f, 32f, 31f, 30f, 24f, 18f, 13f),
                monthlyPrecipitation(1.8f, 1.8f, 2.4f, 0.7f, 1.5f, 1.7f, 1.2f, 1.2f, 0.9f, 1.9f, 1f, 1.3f)
            ),
            Triple(
                "BSk",
                monthlyTemperatures(2f, 4f, 9f, 14f, 19f, 24f, 26f, 25f, 21f, 14f, 8f, 2f),
                monthlyPrecipitation(
                    3.9f,
                    7.1f,
                    19.4f,
                    31.1f,
                    54.6f,
                    65.5f,
                    54f,
                    56f,
                    43.7f,
                    33f,
                    15.3f,
                    6.8f,
                    69.8f,
                    57.5f,
                    26.6f,
                    6.7f,
                    1.2f,
                    0f,
                    0f,
                    0f,
                    0f,
                    5.9f,
                    26.8f,
                    59.1f
                )
            ),

            // Temperate
            Triple(
                "Csa",
                monthlyTemperatures(10, 10, 11, 15, 19, 24, 26, 26, 22, 18, 14, 11),
                monthlyPrecipitation(84.8, 80.9, 55.1, 33.7, 15.9, 6.1, 0.8, 1, 10.5, 34.4, 75.2, 97.5)
            ),
            Triple(
                "Csb",
                monthlyTemperatures(5, 6, 8, 11, 14, 17, 20, 20, 17, 12, 8, 4),
                monthlyPrecipitation(
                    188.9,
                    167.6,
                    145.5,
                    100.6,
                    63.3,
                    41.1,
                    12.8,
                    13.6,
                    44.3,
                    114.1,
                    223.2,
                    221.9,
                    61.4,
                    27.5,
                    0.9,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0.1,
                    9.1,
                    51.9
                )
            ),
            Triple(
                "Cwb",
                monthlyTemperatures(10, 11, 13, 15, 16, 15, 14, 14, 14, 13, 11, 10),
                monthlyPrecipitation(7.8, 6.2, 4.6, 8, 31.8, 110.5, 125.6, 121.1, 111.2, 42.5, 7.6, 2.2)
            ),
            Triple(
                "Cfc",
                monthlyTemperatures(1, 1, 1, 3, 7, 10, 11, 11, 8, 5, 3, 1),
                monthlyPrecipitation(
                    99,
                    102,
                    88,
                    69,
                    56,
                    49,
                    59,
                    80,
                    106,
                    105,
                    103,
                    103,
                    214,
                    199,
                    154,
                    41,
                    2,
                    0,
                    0,
                    0,
                    0,
                    8,
                    61,
                    164
                )
            ),

            // Continental
            Triple(
                "Dsa",
                monthlyTemperatures(-1, 0, 5, 10, 15, 21, 25, 25, 20, 13, 6, 1),
                monthlyPrecipitation(
                    19,
                    23,
                    32,
                    35,
                    24,
                    8,
                    1,
                    1,
                    6,
                    27,
                    34,
                    29,
                    134,
                    123,
                    33,
                    6,
                    0,
                    0,
                    0,
                    0,
                    0,
                    2,
                    28,
                    71
                )
            ),
            Triple(
                "Dsb",
                monthlyTemperatures(-8, -7, -1, 6, 11, 16, 20, 20, 15, 8, 1, -5),
                monthlyPrecipitation(
                    4,
                    6,
                    23,
                    46,
                    43,
                    19,
                    8,
                    5,
                    12,
                    34,
                    31,
                    13,
                    310,
                    362,
                    187,
                    44,
                    1,
                    0,
                    0,
                    0,
                    0,
                    5,
                    70,
                    254
                )
            ),
            Triple(
                "Dwc",
                monthlyTemperatures(-23, -19, -9, 1, 9, 16, 18, 15, 8, -1, -12, -19),
                monthlyPrecipitation(0, 0, 0, 6, 25, 49, 72, 66, 33, 6, 0, 0, 3, 6, 18, 43, 15, 1, 0, 0, 4, 29, 23, 6)
            ),
            Triple(
                "Dfd",
                monthlyTemperatures(-39, -31, -26, -17, -6, 5, 12, 10, 4, -6, -19, -27),
                monthlyPrecipitation(
                    0,
                    0,
                    0,
                    0,
                    10,
                    38,
                    56,
                    53,
                    43,
                    13,
                    1,
                    0,
                    136,
                    131,
                    75,
                    168,
                    178,
                    46,
                    0,
                    0,
                    32,
                    134,
                    138,
                    83
                )
            ),

            // Polar
            Triple(
                "ET",
                monthlyTemperatures(-24, -25, -24, -16, -5, 2, 6, 4, -2, -10, -16, -21),
                monthlyPrecipitation(
                    0,
                    0,
                    0,
                    0,
                    3,
                    17,
                    38,
                    43,
                    16,
                    3,
                    1,
                    1,
                    23,
                    22,
                    17,
                    33,
                    34,
                    19,
                    2,
                    17,
                    82,
                    148,
                    78,
                    42
                )
            ),
            Triple(
                "EF",
                monthlyTemperatures(-24, -25, -24, -16, -5, -2, -1, -1, -2, -10, -16, -21),
                monthlyPrecipitation(
                    0,
                    0,
                    0,
                    0,
                    3,
                    17,
                    38,
                    43,
                    16,
                    3,
                    1,
                    1,
                    23,
                    22,
                    17,
                    33,
                    34,
                    19,
                    2,
                    17,
                    82,
                    148,
                    78,
                    42
                )
            ),
        )

        for ((classification, temperatures, precipitation) in tests) {
            val climate = Meteorology.getKoppenGeigerClimateClassification(
                temperatures,
                precipitation
            )
            assertEquals(classification, climate.code)
        }
    }

    companion object {
        fun provideAltitudes(): List<List<Any?>> {
            return listOf(
                listOf(1000f, 1000f, 0f),
                listOf(1100f, 1000f, -811.3525f),
                listOf(900f, 1000f, 879.9459f),
            )
        }

        fun monthlyTemperatures(
            jan: Number,
            feb: Number,
            mar: Number,
            apr: Number,
            may: Number,
            jun: Number,
            jul: Number,
            aug: Number,
            sep: Number,
            oct: Number,
            nov: Number,
            dec: Number
        ): Map<Month, Temperature> {
            return mapOf(
                Month.JANUARY to Temperature.celsius(jan.toFloat()),
                Month.FEBRUARY to Temperature.celsius(feb.toFloat()),
                Month.MARCH to Temperature.celsius(mar.toFloat()),
                Month.APRIL to Temperature.celsius(apr.toFloat()),
                Month.MAY to Temperature.celsius(may.toFloat()),
                Month.JUNE to Temperature.celsius(jun.toFloat()),
                Month.JULY to Temperature.celsius(jul.toFloat()),
                Month.AUGUST to Temperature.celsius(aug.toFloat()),
                Month.SEPTEMBER to Temperature.celsius(sep.toFloat()),
                Month.OCTOBER to Temperature.celsius(oct.toFloat()),
                Month.NOVEMBER to Temperature.celsius(nov.toFloat()),
                Month.DECEMBER to Temperature.celsius(dec.toFloat())
            )
        }

        fun monthlyPrecipitation(
            jan: Number,
            feb: Number,
            mar: Number,
            apr: Number,
            may: Number,
            jun: Number,
            jul: Number,
            aug: Number,
            sep: Number,
            oct: Number,
            nov: Number,
            dec: Number,
            janSnow: Number = 0,
            febSnow: Number = 0,
            marSnow: Number = 0,
            aprSnow: Number = 0,
            maySnow: Number = 0,
            junSnow: Number = 0,
            julSnow: Number = 0,
            augSnow: Number = 0,
            sepSnow: Number = 0,
            octSnow: Number = 0,
            novSnow: Number = 0,
            decSnow: Number = 0
        ): Map<Month, Distance> {
            return mapOf(
                Month.JANUARY to Distance.from(jan.toFloat() + janSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.FEBRUARY to Distance.from(feb.toFloat() + febSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.MARCH to Distance.from(mar.toFloat() + marSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.APRIL to Distance.from(apr.toFloat() + aprSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.MAY to Distance.from(may.toFloat() + maySnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.JUNE to Distance.from(jun.toFloat() + junSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.JULY to Distance.from(jul.toFloat() + julSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.AUGUST to Distance.from(aug.toFloat() + augSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.SEPTEMBER to Distance.from(sep.toFloat() + sepSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.OCTOBER to Distance.from(oct.toFloat() + octSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.NOVEMBER to Distance.from(nov.toFloat() + novSnow.toFloat() / 10, DistanceUnits.Millimeters),
                Month.DECEMBER to Distance.from(dec.toFloat() + decSnow.toFloat() / 10, DistanceUnits.Millimeters)
            )
        }
        fun provideLowPressures(): List<List<Any?>> {
            return listOf(
                listOf(1000f, true),
                listOf(1009.144f, true),
                listOf(1009.145f, false),
                listOf(1013f, false),
                listOf(1030f, false),
            )
        }
        fun provideHighPressures(): List<List<Any?>> {
            return listOf(
                listOf(1000f, false),
                listOf(1013f, false),
                listOf(1022.688f, false),
                listOf(1022.689f, true),
                listOf(1030f, true),
            )
        }
        fun provideSeaLevelPressure(): List<List<Any?>> {
            return listOf(
                listOf(0f, 0f, null, 0f),
                listOf(0f, 0f, 0f, 0f),
                listOf(1000f, -100f, null, 988.2f),
                listOf(980f, 200f, null, 1003.48f),
                listOf(980f, 1000f, 15f, 1101.93f),
                listOf(1000f, -100f, 28f, 988.71f),
            )
        }

        private val weatherTime = Instant.fromEpochSeconds(100000)
        fun provideWeatherForecasts(): List<List<Any?>> {
            return listOf(
                // Pressure only - storm (warm temps)
                listOf(
                    pressures(1030f, 1021f),
                    emptyList<Reading<CloudGenus?>>(),
                    temperatures(10f, 20f),
                    weather(
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                        WeatherCondition.Rain
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // Pressure only - storm (cold temps)
                listOf(
                    pressures(1030f, 1021f),
                    emptyList<Reading<CloudGenus?>>(),
                    temperatures(-10f, 0f),
                    weather(
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                        WeatherCondition.Snow
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // Pressure only - storm (slightly warm temps)
                listOf(
                    pressures(1030f, 1021f),
                    emptyList<Reading<CloudGenus?>>(),
                    temperatures(-2f, 8f),
                    weather(
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // Pressure only - storm (slightly cold temps)
                listOf(
                    pressures(1030f, 1021f),
                    emptyList<Reading<CloudGenus?>>(),
                    temperatures(-5f, 2f),
                    weather(
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // Pressure only - worsening slow
                listOf(
                    pressures(1030f, 1027f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        WeatherFront.Warm,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Falling, -1f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Overcast,
                    ),
                    weatherLater(
                        null
                    )
                ),
                // Pressure only - worsening fast
                listOf(
                    pressures(1009f, 1005f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        WeatherFront.Warm,
                        PressureSystem.Low,
                        PressureTendency(PressureCharacteristic.FallingFast, -4 / 3f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),

                // Pressure only - improving slow
                listOf(
                    pressures(1027f, 1030f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        WeatherFront.Cold,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Rising, 1f),
                        WeatherCondition.Clear
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // Pressure only - improving slow (not high pressure)
                listOf(
                    pressures(1014f, 1017f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.Rising, 1f)
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // Pressure only - improving fast
                listOf(
                    pressures(1005f, 1009f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weatherAt(
                        weatherTime,
                        WeatherFront.Cold,
                        PressureSystem.Low,
                        PressureTendency(PressureCharacteristic.RisingFast, 4 / 3f),
                        WeatherCondition.Wind
                    ),
                    weatherLater(
                        null
                    )
                ),

                // clouds only - warm front
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Stratus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // clouds only - warm front (Ns)
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Nimbostratus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds,
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // clouds only - cold front
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulus),
                    temperatures(10f, 20f),
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Storm,
                        WeatherCondition.Thunderstorm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                        WeatherCondition.Rain
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // clouds only - cold front (Cb)
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulonimbus),
                    temperatures(10f, 20f),
                    weatherAt(
                        weatherTime - 1.seconds,
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Storm,
                        WeatherCondition.Thunderstorm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                        WeatherCondition.Rain
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // clouds only - cold front (Cb only)
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cumulonimbus),
                    temperatures(10f, 20f),
                    weatherAt(
                        weatherTime - 1.seconds,
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Storm,
                        WeatherCondition.Thunderstorm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast,
                        WeatherCondition.Rain
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // clouds only - cold front (Cb, cold temps)
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulonimbus),
                    temperatures(-10f, 10f),
                    weatherAt(
                        weatherTime - 1.seconds,
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // clouds only - unknown front
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + (Time.hours(12.0) - 500.milliseconds),
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        null
                    )
                ),
                // clouds only - no front
                listOf(
                    emptyList<Reading<Pressure>>(),
                    clouds(CloudGenus.Cumulus, CloudGenus.Stratocumulus),
                    null,
                    weatherAt(
                        weatherTime,
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        null
                    )
                ),

                // no data
                listOf(
                    emptyList<Reading<Pressure>>(),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f)
                    ),
                    weatherLater(
                        null
                    )
                ),

                // warm front (slow)
                listOf(
                    pressures(1030f, 1027f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Stratus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Warm,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Falling, -1f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        null
                    )
                ),
                // warm front (fast)
                listOf(
                    pressures(1009f, 1005f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Stratus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Warm,
                        PressureSystem.Low,
                        PressureTendency(PressureCharacteristic.FallingFast, -4 / 3f),
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // warm front (storm)
                listOf(
                    pressures(1030f, 1021f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Stratus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Warm,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),
                // cold front (slow)
                listOf(
                    pressures(1030f, 1027f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Cold,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Falling, -1f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),
                // cold front (fast)
                listOf(
                    pressures(1009f, 1005f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Cold,
                        PressureSystem.Low,
                        PressureTendency(PressureCharacteristic.FallingFast, -4 / 3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        null
                    )
                ),
                // cold front (storm)
                listOf(
                    pressures(1030f, 1021f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulus),
                    null,
                    weatherAt(
                        weatherTime - 1.seconds + Time.hours(1.5),
                        WeatherFront.Cold,
                        null,
                        PressureTendency(PressureCharacteristic.FallingFast, -3f),
                        WeatherCondition.Storm,
                        WeatherCondition.Precipitation,
                        WeatherCondition.Wind,
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),

                // Cold front (passing)
                listOf(
                    pressures(1027f, 1030f),
                    clouds(CloudGenus.Cirrus, CloudGenus.Altocumulus, CloudGenus.Cumulus),
                    null,
                    weather(
                        WeatherFront.Cold,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Rising, 1f),
                        WeatherCondition.Clear
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),

                // Overcast in high pressure
                listOf(
                    pressures(1030f, 1030f),
                    clouds(CloudGenus.Stratus),
                    null,
                    weatherAt(
                        weatherTime,
                        null,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Overcast
                    ),
                    weatherLater(
                        PressureSystem.High,
                        WeatherCondition.Clear
                    )
                ),

                // Clear in low pressure
                listOf(
                    pressures(1000f, 1000f),
                    clouds(null),
                    null,
                    weatherAt(
                        weatherTime,
                        null,
                        PressureSystem.Low,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Clear
                    ),
                    weatherLater(
                        PressureSystem.Low,
                        WeatherCondition.Overcast
                    )
                ),

                // Unknown
                listOf(
                    pressures(1022f, 1022f),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f)
                    ),
                    weatherLater(null)
                ),

                // Replaces unknown forecast (using pressure)
                listOf(
                    pressures(
                        1024f,
                        1023f
                    ).map { it.copy(time = it.time - 6.hours) } + pressures(
                        1022f,
                        1022f
                    ),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Clear
                    ),
                    weatherLater(null)
                ),

                // Replaces unknown forecast (using clouds)
                listOf(
                    pressures(
                        1022f,
                        1022f
                    ),
                    clouds(CloudGenus.Stratus).map { it.copy(time = it.time - 6.hours) },
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Overcast
                    ),
                    weatherLater(null)
                ),

                // Ignores front-indicating clouds older than 24 hours
                listOf(
                    emptyList<Reading<Pressure>>(),
                    listOf(
                        Reading(CloudGenus.Cirrus, weatherTime - 26.hours),
                        Reading(CloudGenus.Altocumulus, weatherTime - 25.hours),
                        Reading(
                            CloudGenus.Stratus,
                            weatherTime - 24.hours - 1.seconds
                        )
                    ),
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f)
                    ),
                    weatherLater(null)
                ),

                // Ignores current cloud conditions older than 3 hours
                listOf(
                    pressures(1030f, 1030f),
                    listOf(
                        Reading(CloudGenus.Stratus, weatherTime - 4.hours)
                    ),
                    null,
                    weatherAt(
                        weatherTime,
                        null,
                        PressureSystem.High,
                        PressureTendency(PressureCharacteristic.Steady, 0f),
                        WeatherCondition.Clear
                    ),
                    weatherLater(
                        PressureSystem.High
                    )
                ),

                // Ignores pressures older than 48 hours
                listOf(
                    listOf(
                        Reading(Pressure.hpa(1027f), weatherTime - 52.hours),
                        Reading(Pressure.hpa(1030f), weatherTime - 49.hours)
                    ),
                    emptyList<Reading<CloudGenus?>>(),
                    null,
                    weather(
                        null,
                        null,
                        PressureTendency(PressureCharacteristic.Steady, 0f)
                    ),
                    weatherLater(null)
                )
            )
        }

        private fun temperatures(low: Float, high: Float): Range<Temperature> {
            return Range(Temperature.celsius(low), Temperature.celsius(high))
        }

        private fun weatherLater(
            system: PressureSystem?,
            vararg conditions: WeatherCondition
        ): WeatherForecast {
            return WeatherForecast(
                null,
                conditions.toList(),
                null,
                system,
                null
            )
        }

        private fun weatherAt(
            time: Instant,
            front: WeatherFront?,
            system: PressureSystem?,
            tendency: PressureTendency,
            vararg conditions: WeatherCondition
        ): WeatherForecast {
            return WeatherForecast(
                time,
                conditions.toList(),
                front,
                system,
                tendency
            )
        }

        private fun weather(
            front: WeatherFront?,
            system: PressureSystem?,
            tendency: PressureTendency,
            vararg conditions: WeatherCondition
        ): WeatherForecast {
            return WeatherForecast(
                null,
                conditions.toList(),
                front,
                system,
                tendency
            )
        }

        private fun pressures(last: Float, now: Float): List<Reading<Pressure>> {
            val time = weatherTime
            return listOf(
                Reading(Pressure.hpa(last), time - 3.hours),
                Reading(Pressure.hpa(now), time)
            )
        }

        private fun clouds(vararg clouds: CloudGenus?): List<Reading<CloudGenus?>> {
            var time = weatherTime - clouds.size.seconds
            return clouds.map {
                val r = Reading(it, time)
                time += 1.seconds
                r
            }
        }
        fun provideForecasts(): List<List<Any?>> {
            // First argument not used yet
            return listOf(
                listOf(PressureCharacteristic.Steady, 0f, -6f, Weather.NoChange),
                listOf(PressureCharacteristic.FallingFast, -6f, -6f, Weather.Storm),
                listOf(PressureCharacteristic.FallingFast, -8f, -7f, Weather.Storm),
                listOf(PressureCharacteristic.FallingFast, -6 / 3f, null, Weather.Storm),
                listOf(
                    PressureCharacteristic.FallingFast,
                    -5 / 3f,
                    null,
                    Weather.WorseningFast
                ),
                listOf(
                    PressureCharacteristic.FallingFast,
                    -4f,
                    -6f,
                    Weather.WorseningFast
                ),
                listOf(PressureCharacteristic.Falling, -2f, -6f, Weather.WorseningSlow),
                listOf(PressureCharacteristic.Rising, 2f, -6f, Weather.ImprovingSlow),
                listOf(
                    PressureCharacteristic.RisingFast,
                    8f,
                    -6f,
                    Weather.ImprovingFast
                ),
            )
        }
        fun provideTendencies(): List<List<Any?>> {
            return listOf(
                listOf(
                    1000f,
                    1000f,
                    3.hours,
                    2f,
                    PressureTendency(PressureCharacteristic.Steady, 0f)
                ),
                listOf(
                    1000f,
                    1001f,
                    3.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.Steady, 1 / 3f)
                ),
                listOf(
                    1000f,
                    1004f,
                    3.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.RisingFast, 4 / 3f)
                ),
                listOf(
                    1000f,
                    1003f,
                    3.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.Rising, 1f)
                ),
                listOf(
                    1004f,
                    1000f,
                    3.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.FallingFast, -4 / 3f)
                ),
                listOf(
                    1002f,
                    1000f,
                    3.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.Falling, -2 / 3f)
                ),
                listOf(
                    1002f,
                    1000f,
                    3.hours,
                    1 / 3f,
                    PressureTendency(PressureCharacteristic.Falling, -2 / 3f)
                ),
                listOf(
                    1003f,
                    1000f,
                    3.hours,
                    1 / 3f,
                    PressureTendency(PressureCharacteristic.FallingFast, -1f)
                ),
                listOf(
                    1002f,
                    1000f,
                    2.hours,
                    1 / 3f,
                    PressureTendency(PressureCharacteristic.FallingFast, -1f)
                ),
                listOf(
                    1008f,
                    1000f,
                    4.hours,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.FallingFast, -2f)
                ),
                listOf(
                    1000f,
                    1000f,
                    Duration.ZERO,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.Steady, 0f)
                ),
                listOf(
                    1000.1f,
                    1000f,
                    Duration.ZERO,
                    2 / 3f,
                    PressureTendency(PressureCharacteristic.Steady, 0f)
                ),
            )
        }
        fun provideHeatIndex(): List<List<Any?>> {
            return listOf(
                listOf(0f, 0f, 0f),
                listOf(1f, 0f, 1f),
                listOf(26f, 0f, 26f),
                listOf(27f, 80f, 30f),
                listOf(30f, 80f, 38f),
                listOf(30f, 100f, 44f),
                listOf(30f, 45f, 30f),
                listOf(36f, 60f, 48f),
            )
        }
        fun provideHeatAlert(): List<List<Any?>> {
            return listOf(
                listOf(-26f, HeatAlert.FrostbiteDanger),
                listOf(-25f, HeatAlert.FrostbiteDanger),
                listOf(-24f, HeatAlert.FrostbiteWarning),
                listOf(-17f, HeatAlert.FrostbiteWarning),
                listOf(-16f, HeatAlert.FrostbiteCaution),
                listOf(5f, HeatAlert.FrostbiteCaution),
                listOf(6f, HeatAlert.Normal),
                listOf(26f, HeatAlert.Normal),
                listOf(27f, HeatAlert.HeatCaution),
                listOf(32.5f, HeatAlert.HeatCaution),
                listOf(33f, HeatAlert.HeatWarning),
                listOf(39f, HeatAlert.HeatWarning),
                listOf(40f, HeatAlert.HeatAlert),
                listOf(50f, HeatAlert.HeatAlert),
                listOf(51f, HeatAlert.HeatDanger),
            )
        }
        fun provideDewPoint(): List<List<Any?>> {
            return listOf(
                listOf(20f, 100f, 20f),
                listOf(21f, 90f, 19f),
                listOf(15f, 24f, -5f),
                listOf(16f, 10f, -16f),
            )
        }
        fun provideLightningStrikes(): List<List<Any?>> {
            return listOf(
                listOf(Instant.fromEpochSeconds(0), Instant.fromEpochSeconds(1), 343f),
                listOf(Instant.fromEpochSeconds(0), Instant.fromEpochSeconds(2), 686f),
                listOf(Instant.fromEpochSeconds(0), Instant.fromEpochSeconds(10), 3430f),
                listOf(Instant.fromEpochSeconds(0), Instant.fromEpochSeconds(0), 0f),
                listOf(Instant.fromEpochSeconds(1), Instant.fromEpochSeconds(0), 0f),
            )
        }
        fun provideLightningStrikeDistances(): List<List<Any?>> {
            return listOf(
                listOf(Distance.kilometers(10f).meters().value, true),
                listOf(Distance.kilometers(10.1f).meters().value, false),
                listOf(10000f, true),
                listOf(100f, true),
            )
        }
        fun provideSeasons(): List<List<Any?>> {
            return listOf(
                listOf(Season.Summer, false, LocalDate(2021, 1, 1)),
                listOf(Season.Summer, false, LocalDate(2021, 2, 28)),
                listOf(Season.Fall, false, LocalDate(2021, 3, 1)),
                listOf(Season.Fall, false, LocalDate(2021, 5, 31)),
                listOf(Season.Winter, false, LocalDate(2021, 6, 1)),
                listOf(Season.Winter, false, LocalDate(2021, 8, 31)),
                listOf(Season.Spring, false, LocalDate(2021, 9, 1)),
                listOf(Season.Spring, false, LocalDate(2021, 11, 30)),
                listOf(Season.Summer, false, LocalDate(2021, 12, 1)),
                listOf(Season.Summer, false, LocalDate(2021, 12, 31)),

                listOf(Season.Winter, true, LocalDate(2021, 1, 1)),
                listOf(Season.Winter, true, LocalDate(2021, 2, 28)),
                listOf(Season.Spring, true, LocalDate(2021, 3, 1)),
                listOf(Season.Spring, true, LocalDate(2021, 5, 31)),
                listOf(Season.Summer, true, LocalDate(2021, 6, 1)),
                listOf(Season.Summer, true, LocalDate(2021, 8, 31)),
                listOf(Season.Fall, true, LocalDate(2021, 9, 1)),
                listOf(Season.Fall, true, LocalDate(2021, 11, 30)),
                listOf(Season.Winter, true, LocalDate(2021, 12, 1)),
                listOf(Season.Winter, true, LocalDate(2021, 12, 31)),
            )
        }

    }

}