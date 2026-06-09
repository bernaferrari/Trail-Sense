package com.kylecorry.trail_sense.tools.weather.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.charts.WeatherLineChart
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

        val readings = forecast.map { reading ->
            Reading(
                reading.value.convertTo(temperatureUnits).value,
                reading.time
            )
        }

        CustomUiUtils.showComposeContentDialog(
            context = context,
            title = context.getString(R.string.next_24_hours),
            content = context.getString(
                R.string.historic_temperatures_full_disclaimer,
                30
            ) + "\n\n" + context.getString(
                R.string.now_value,
                now
            )
        ) {
            WeatherLineChart(
                readings = readings,
                freezingValue = Temperature.celsius(0f).convertTo(temperatureUnits).value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
            )
        }
    }
}
