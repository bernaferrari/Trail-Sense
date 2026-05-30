package com.kylecorry.trail_sense.tools.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

/**
 * Pure Compose Weather screen (no Fragment, no hybrid).
 * Full feature parity with the old implementation is in progress as part of the 100% Compose migration.
 */
@Composable
fun WeatherScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val weatherSubsystem = remember { WeatherSubsystem.getInstance(context) }
    val formatService = remember { FormatService.getInstance(context) }

    var currentWeather by remember { mutableStateOf<CurrentWeather?>(null) }
    var pressureHistory by remember { mutableStateOf<List<Reading<Pressure>>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Initial load (pure Compose, no Fragment)
        currentWeather = weatherSubsystem.getWeather()
        // Use available history API
        val history = weatherSubsystem.getHistory()
        pressureHistory = history.map { it.pressureReading() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TrailSenseSpacing.md)
    ) {
        Text(
            "Weather",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = TrailSenseSpacing.lg)
        )

        // Current conditions card (basic)
        TrailSenseCard {
            Column(modifier = Modifier.padding(TrailSenseSpacing.md)) {
                Text(
                    "Current",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(TrailSenseSpacing.sm))

                currentWeather?.observation?.let { obs ->
                    Text(
                        "Latest observation available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } ?: Text("Loading weather data...")
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.lg))

        // Quick status / monitor placeholder (can be expanded)
        TrailSenseCard {
            Column(modifier = Modifier.padding(TrailSenseSpacing.md)) {
                Text("Monitor", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Weather monitoring controls will live here (play bar, frequency, etc.).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.lg))

        // History / charts placeholder
        TrailSenseCard {
            Column(modifier = Modifier.padding(TrailSenseSpacing.md)) {
                Text("History", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${pressureHistory.size} pressure readings loaded. Full charts (Temperature, Pressure, Humidity) coming in next pass.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
