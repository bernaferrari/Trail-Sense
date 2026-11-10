package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import kotlin.time.Clock
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import com.kylecorry.andromeda.core.time.ZonedDateTime
import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

internal class DailyWeatherAlertCommandTest {

    private lateinit var command: DailyWeatherAlertCommand
    private lateinit var prefs: IWeatherPreferences
    private lateinit var alerter: IValueAlerter<WeatherPrediction>
    private lateinit var timeProvider: ZonedDateTimeProvider
    private lateinit var weather: CurrentWeather

    @BeforeEach
    fun setup() {
        weather = CurrentWeather(
            WeatherPrediction(
                emptyList(),
                emptyList(),
                null,
                WeatherArrivalTime(Clock.System.now(), false),
                null,
                emptyList()
            ),
            PressureTendency(PressureCharacteristic.Steady, 0f),
            null,
            null
        )

        prefs = mock()
        alerter = mock()
        timeProvider = mock()

        whenever(timeProvider.getTime()).thenReturn(
            ZonedDateTime(
                LocalDateTime(2022, 1, 1, 7, 0).toInstant(TimeZone.UTC),
                TimeZone.UTC
            )
        )

        whenever(prefs.dailyForecastTime).thenReturn(LocalTime(6, 0))
        whenever(prefs.shouldMonitorWeather).thenReturn(true)
        whenever(prefs.shouldShowDailyWeatherNotification).thenReturn(true)
        whenever(prefs.dailyWeatherIsForTomorrow).thenReturn(false)
        whenever(prefs.dailyWeatherLastSent).thenReturn(null)

        command = DailyWeatherAlertCommand(prefs, alerter, timeProvider)
    }

    @Test
    fun shouldNotSendIfDisabled() {
        whenever(prefs.shouldShowDailyWeatherNotification).thenReturn(false)

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldNotSendIfWeatherMonitorOff() {
        whenever(prefs.shouldMonitorWeather).thenReturn(false)

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldNotSendIfAlreadySent() {
        whenever(prefs.dailyWeatherLastSent).thenReturn(LocalDate(2022, 1, 1))

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldNotSendIfTooEarly() {
        whenever(prefs.dailyForecastTime).thenReturn(LocalTime(12, 0))

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldNotSendIfTooLate() {
        whenever(prefs.dailyForecastTime).thenReturn(LocalTime(2, 0))

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldSendAlert() {
        command.execute(weather)

        verify(prefs).dailyWeatherLastSent = LocalDate(2022, 1, 1)
        verify(alerter, times(1)).alert(weather.prediction)
    }

}
