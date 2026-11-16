package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.subscriptions.ISubscription
import com.kylecorry.luna.subscriptions.Subscription
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateClassification
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.time.today
import com.kylecorry.trail_sense.tools.climate.infrastructure.SharedClimateRepo
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.weather.WeatherBroadcasts
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.IWeatherForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.MonteCarloPressureForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.WeatherForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.CalibratedTemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.HistoricTemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Multiplatform [IWeatherSubsystem] using Room weather history, shared climate assets, and hardware sensors.
 */
class SharedWeatherSubsystem(
    private val weatherRepo: IWeatherRepo,
    private val cloudRepo: CloudRepo,
    private val climate: SharedClimateRepo,
    private val prefs: UserPreferences,
    private val sharedPrefs: Preferences,
    private val location: ILocationSubsystem,
    private val sensorService: SensorService
) : IWeatherSubsystem {

    private var cachedValue = MemoryCachedValue<CurrentWeather>()
    private val validLock = SynchronizedObject()
    private var isValid = false
    private val updateWeatherMutex = Mutex()

    private val readingsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _weatherChanged = Subscription()
    override val weatherChanged: ISubscription = _weatherChanged

    private val invalidationPrefKeys = listOf(
        PreferenceKeys.useSeaLevelPressure,
        PreferenceKeys.barometerPressureSmoothing,
        PreferenceKeys.adjustForTemperature,
        PreferenceKeys.forecastSensitivity,
        PreferenceKeys.stormAlertSensitivity,
        PreferenceKeys.altimeterCalibrationMode,
        PreferenceKeys.pressureHistory,
        PreferenceKeys.temperatureSmoothing,
        PreferenceKeys.barometerOffset,
        PreferenceKeys.pressureUnits
    )

    init {
        sharedPrefs.onEachChange { key ->
            if (key in invalidationPrefKeys) {
                invalidate()
            }
        }
        readingsScope.launch(start = CoroutineStart.UNDISPATCHED) {
            merge(weatherRepo.readingsChanged, cloudRepo.readingsChanged).collect { invalidate() }
        }
        weatherChanged.subscribe {
            ToolBroadcasts.broadcast(WeatherBroadcasts.PREDICTION_CHANGED)
        }
    }

    override suspend fun getWeather(): CurrentWeather = onIO {
        if (!synchronized(validLock) { isValid }) {
            refresh()
        }
        cachedValue.getOrPut { populateCache() }
    }

    override suspend fun getHistory(): List<WeatherObservation> = onIO {
        val readings = getRawHistory()
        val precalibrated = calibrateHumidity(calibrateTemperatures(readings))
        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        val pressures = calibrator.calibrate(precalibrated)
        val offset = prefs.weather.barometerOffset

        pressures.map {
            val reading = precalibrated.firstOrNull { r -> r.time == it.time }
            WeatherObservation(
                reading?.value?.id ?: 0L,
                it.time,
                Pressure.from(it.value.value + offset, it.value.units),
                Temperature.celsius(reading?.value?.temperature ?: 0f),
                reading?.value?.humidity
            )
        }
    }

    override suspend fun getTemperature(
        time: Instant,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): Reading<Temperature> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        Reading(service.getTemperature(time), time)
    }

    override suspend fun getTemperatures(
        start: Instant,
        end: Instant,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): List<Reading<Temperature>> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatures(start, end)
    }

    override suspend fun getTemperatureRange(
        date: LocalDate,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): Range<Temperature> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatureRange(date)
    }

    override suspend fun getTemperatureRanges(
        year: Int,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatureRanges(year)
    }

    private suspend fun resolveLocation(
        location: Coordinate?,
        elevation: Distance?
    ): Pair<Coordinate, Distance> {
        if (location != null && elevation != null) {
            return location to elevation
        }
        val last = weatherRepo.getLast()
        val lookupLocation = location ?: last?.value?.location ?: this.location.location
        val lookupElevation = elevation
            ?: last?.value?.altitude?.let { Distance.meters(it) }
            ?: this.location.elevation
        return lookupLocation to lookupElevation
    }

    override suspend fun getRawHistory(applyPressureOffset: Boolean): List<Reading<RawWeatherObservation>> =
        onIO {
            if (!synchronized(validLock) { isValid }) {
                refresh()
            }
            val now = Clock.System.now()
            val readings = weatherRepo.getAll()
                .asSequence()
                .sortedBy { it.time }
                .filter { it.time <= now }
                .map {
                    if (it.value.location == Coordinate.zero) {
                        it.copy(value = it.value.copy(location = location.location))
                    } else {
                        it
                    }
                }
                .toList()

            if (applyPressureOffset) {
                calibrateBarometerOffset(readings)
            } else {
                readings
            }
        }

    override suspend fun getMonthlyPrecipitation(location: Coordinate?): Map<Month, Distance> {
        val resolved = resolveLocation(location, null)
        return climate.getMonthlyPrecipitation(resolved.first)
    }

    override suspend fun getClimateClassification(
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): KoppenGeigerClimateClassification {
        val temperatures = getTemperatureRanges(today().year, location, elevation, calibrated)
        val precipitation = getMonthlyPrecipitation(location)
        val monthlyAverageTemperatures = temperatures
            .filter { it.first.day == 15 }
            .associate {
                it.first.month to Temperature.celsius((it.second.start.celsius().value + it.second.end.celsius().value) / 2f)
            }
        return Meteorology.getKoppenGeigerClimateClassification(
            monthlyAverageTemperatures,
            precipitation
        )
    }

    override suspend fun updateWeather() = onDefault {
        updateWeatherMutex.withLock {
            if (!prefs.device.hasBarometer || !prefs.weather.hasBarometer) {
                return@onDefault
            }
            val now = Clock.System.now()
            val last = weatherRepo.getLast()?.time
            val maxPeriod = prefs.weather.weatherUpdateFrequency.div(3)
            if (last != null && (now - last).absoluteValue < maxPeriod) {
                return@onDefault
            }

            val barometer = sensorService.getBarometer(calibrated = false)
            val thermometer = sensorService.getThermometer(calibrated = true)
            val hygrometer = sensorService.getHygrometer()
            val altimeter = sensorService.getAltimeter()
            withTimeoutOrNull(5.seconds) {
                readAll(listOfNotNull(barometer, thermometer, hygrometer, altimeter), onlyIfInvalid = false)
            }

            val pressure = barometer.pressure
            if (barometer.hasValidReading && pressure > 0f) {
                val observation = RawWeatherObservation(
                    id = 0,
                    pressure = pressure,
                    altitude = altimeter.altitude,
                    temperature = thermometer.temperature,
                    humidity = hygrometer.humidity.takeIf { prefs.device.hasHygrometer && hygrometer.hasValidReading },
                    location = location.location
                )
                weatherRepo.add(Reading(observation, Clock.System.now()))
            }
        }
    }

    private suspend fun getTemperatureService(
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): ITemperatureService {
        val resolved = resolveLocation(location, elevation)
        val service = HistoricTemperatureService(climate, resolved.first, resolved.second)
        return if (calibrated) {
            CalibratedTemperatureService(service, prefs.thermometer.calibrator)
        } else {
            service
        }
    }

    private suspend fun getWeatherForecaster(
        location: Coordinate?,
        elevation: Distance?
    ): IWeatherForecaster {
        val resolved = resolveLocation(location, elevation)
        val temperatureService = getTemperatureService(resolved.first, resolved.second, true)
        return WeatherForecaster(temperatureService, prefs.weather, ::forecastSource)
    }

    private fun forecastSource(): ForecastSource {
        return when (sharedPrefs.getString(PreferenceKeys.weatherForecastSource)) {
            "2" -> ForecastSource.Zambretti
            else -> ForecastSource.Sol
        }
    }

    private fun invalidate() {
        synchronized(validLock) {
            isValid = false
        }
        _weatherChanged.publish()
    }

    private suspend fun refresh() {
        cachedValue.reset()
        delay(50)
        synchronized(validLock) {
            isValid = true
        }
    }

    override suspend fun getCloudHistory(): List<Reading<CloudGenus?>> = onIO {
        cloudRepo.getAll().sortedBy { it.time }.map { Reading(it.value.genus, it.time) }
    }

    override suspend fun getPressureForecast(): List<Reading<Pressure>> = onDefault {
        val forecaster = MonteCarloPressureForecaster()
        val history = getHistory().map { it.pressureReading() }.takeLast(50)
        val isPressureTrusted = prefs.weather.pressureSmoothing < 0.15f
        val derivativeSmoothing = if (isPressureTrusted) 0f else 0.1f
        val error = if (isPressureTrusted) 0.1f else 0.2f
        forecaster.getPressureForecast(
            history,
            maxErrorHpa = 8f,
            velocitySmoothing = derivativeSmoothing,
            accelerationSmoothing = derivativeSmoothing,
            velocityError = error,
            accelerationError = error
        )
    }

    private suspend fun populateCache(): CurrentWeather {
        val history = getHistory()
        val allClouds = getCloudHistory()
        val last = history.lastOrNull()
        val (lookupLocation, lookupElevation) = getLocationAndElevation(last?.id)
        val forecaster = getWeatherForecaster(lookupLocation, lookupElevation)
        return forecaster.forecast(history, allClouds, lookupLocation)
    }

    private suspend fun getLocationAndElevation(lastReadingId: Long? = null): Pair<Coordinate, Distance> {
        val lastRawReading = lastReadingId?.let { weatherRepo.get(it) }
        val resolvedLocation = lastRawReading?.value?.location ?: location.location
        val resolvedElevation = lastRawReading?.value?.altitude?.let { Distance.meters(it) }
            ?: this.location.elevation
        return resolvedLocation to resolvedElevation
    }

    private fun calibrateTemperatures(
        readings: List<Reading<RawWeatherObservation>>
    ): List<Reading<RawWeatherObservation>> {
        return DataUtils.smoothTemporal(
            readings,
            prefs.thermometer.smoothing,
            { it.temperature }
        ) { reading, smoothed ->
            reading.copy(temperature = smoothed)
        }
    }

    private fun calibrateHumidity(
        readings: List<Reading<RawWeatherObservation>>
    ): List<Reading<RawWeatherObservation>> {
        if (!prefs.device.hasHygrometer) {
            return readings
        }
        return DataUtils.smoothTemporal(
            readings,
            0.1f,
            { it.humidity ?: 0f }
        ) { reading, smoothed ->
            reading.copy(humidity = if (smoothed == 0f) null else smoothed)
        }
    }

    private fun calibrateBarometerOffset(
        readings: List<Reading<RawWeatherObservation>>
    ): List<Reading<RawWeatherObservation>> {
        val offset = prefs.weather.barometerOffset
        if (offset == 0f) {
            return readings
        }
        return readings.map { it.copy(value = it.value.copy(pressure = it.value.pressure + offset)) }
    }
}
