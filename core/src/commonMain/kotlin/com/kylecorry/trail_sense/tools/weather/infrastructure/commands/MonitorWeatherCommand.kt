package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem

class MonitorWeatherCommand(
    private val repo: IReadingRepo<RawWeatherObservation>,
    private val observer: IWeatherObserver,
    private val subsystem: IWeatherSubsystem,
    private val alerter: Command<CurrentWeather>
) : CoroutineCommand {

    override suspend fun execute() {
        updateWeather()
        sendWeatherNotifications()
    }

    private suspend fun updateWeather() {
        val reading = observer.getWeatherObservation()
        reading?.let { repo.add(it) }
    }

    private suspend fun sendWeatherNotifications() {
        val weather = subsystem.getWeather()
        alerter.execute(weather)
    }
}
