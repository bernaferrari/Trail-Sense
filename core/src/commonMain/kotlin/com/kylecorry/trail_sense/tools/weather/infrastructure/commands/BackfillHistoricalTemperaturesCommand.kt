package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem

/** Fills in each stored weather reading's temperature from historic temperatures at its time and place. */
class BackfillHistoricalTemperaturesCommand(
    private val weather: IWeatherSubsystem = getAppService(),
    private val repo: IWeatherRepo = getAppService()
) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val readings = weather.getRawHistory()

        val updated = readings.map {
            val temperature = weather.getTemperature(
                it.time,
                it.value.location,
                Distance.meters(it.value.altitude)
            )
            it.copy(value = it.value.copy(temperature = temperature.value.value))
        }
        repo.addAll(updated)
    }
}
