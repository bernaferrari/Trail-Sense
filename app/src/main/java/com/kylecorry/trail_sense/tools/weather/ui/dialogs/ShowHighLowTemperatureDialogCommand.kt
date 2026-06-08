package com.kylecorry.trail_sense.tools.weather.ui.dialogs

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.charts.TemperatureChart
import java.time.ZonedDateTime

class ShowHighLowTemperatureDialogCommand(
    private val context: Context,
    private val location: Coordinate? = null,
    private val elevation: Distance? = null
) : CoroutineCommand {

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(context) }
    private val formatter by lazy { FormatService.getInstance(context) }
    private val temperatureUnits by lazy { UserPreferences(context).temperatureUnits }

    override suspend fun execute() {
        val forecast =
            onIO {
                weatherSubsystem.getTemperatures(
                    ZonedDateTime.now(),
                    ZonedDateTime.now().plusHours(24),
                    location,
                    elevation
                )
            }

        val now = formatter.formatTemperature(forecast.first().value.convertTo(temperatureUnits))

        CustomUiUtils.showChart(
            context,
            context.getString(R.string.next_24_hours),
            context.getString(
                R.string.historic_temperatures_full_disclaimer,
                30
            ) + "\n\n" + context.getString(
                R.string.now_value,
                now
            )
        ) {
            val chart = TemperatureChart(it)
            chart.plot(forecast.map { reading ->
                Reading(
                    reading.value.convertTo(temperatureUnits).value,
                    reading.time
                )
            })
        }
    }
}
