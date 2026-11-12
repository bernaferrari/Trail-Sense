package com.kylecorry.trail_sense.tools.weather.ui.dialogs

import com.kylecorry.trail_sense.shared.views.compose.chartHeight

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.dialogs.showComposeContentDialog
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.views.charts.TrailSenseLineChart
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class ShowHighLowTemperatureDialogCommand(
    private val host: AppHost = AppHost.current(),
    private val location: Coordinate? = null,
    private val elevation: Distance? = null
) : CoroutineCommand {

    private val weatherSubsystem by lazy { getAppService<IWeatherSubsystem>() }
    private val formatter by lazy { FormatService.getInstance() }
    private val temperatureUnits by lazy { getAppService<UserPreferences>().temperatureUnits }

    override suspend fun execute() {
        val forecast =
            onIO {
                weatherSubsystem.getTemperatures(
                    Clock.System.now(),
                    Clock.System.now().plus(24.hours),
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

        val strings = getAppService<AppStrings>()
        showComposeContentDialog(
            host = host,
            title = strings.get(Res.string.next_24_hours),
            content = strings.get(Res.string.historic_temperatures_full_disclaimer, 30) +
                    "\n\n" + strings.get(Res.string.now_value, now)
        ) {
            TrailSenseLineChart(
                readings = readings,
                freezingValue = Temperature.celsius(0f).convertTo(temperatureUnits).value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight())
                    .testTag("chart")
            )
        }
    }
}
