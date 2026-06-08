package com.kylecorry.trail_sense.tools.experimentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.service.WeatherForecastService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Composable
internal fun ExperimentationContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val pluginSubsystem = remember { PluginSubsystem.getInstance(context) }
    val locationSubsystem = remember { LocationSubsystem.getInstance(context) }

    var isLoading by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<Forecast?>(null) }
    var pluginsText by remember { mutableStateOf("") }
    var weatherService by remember { mutableStateOf<WeatherForecastService?>(null) }

    LaunchedEffect(pluginSubsystem) {
        val plugins = withContext(Dispatchers.IO) {
            pluginSubsystem.getPluginResourceServiceDetails()
        }
        pluginsText = plugins.joinToString("\n\n")
        weatherService = plugins.firstOrNull { it.features.weather.isNotEmpty() }?.let {
            WeatherForecastService(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        Text(
            text = getWeatherText(isLoading, weather, formatter, prefs),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    tryOrLog {
                        weather = withContext(Dispatchers.IO) {
                            weatherService?.getWeather(locationSubsystem.location)
                        }
                    }
                    isLoading = false
                }
            }
        ) {
            Text("Update")
        }
        Text(
            text = weather?.citation.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = pluginsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun getWeatherText(
    isLoading: Boolean,
    weather: Forecast?,
    formatter: FormatService,
    prefs: UserPreferences
): String {
    return if (isLoading) {
        "Loading"
    } else if (weather == null) {
        "No data"
    } else {
        formatter.join(
            formatter.formatTemperature(
                Temperature.celsius(weather.current.temperature ?: 0f)
                    .convertTo(prefs.temperatureUnits)
            ),
            formatter.formatPercentage(weather.current.humidity ?: 0f),
            formatter.formatSpeed(
                Speed.from(
                    weather.current.windSpeed ?: 0f,
                    DistanceUnits.Kilometers,
                    TimeUnits.Hours
                ).convertTo(
                    DistanceUnits.Meters,
                    TimeUnits.Seconds
                ).value
            ),
            formatter.formatWeather(weather.current.weather),
            separator = FormatService.Separator.NewLine
        )
    }
}
