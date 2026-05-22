package com.kylecorry.trail_sense.tools.experimentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.service.WeatherForecastService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.ExperimentationContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }

    var isLoading by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<Forecast?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Text("Experimentation / Plugin Sample (Pure Compose)")
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { }) {
            Text("Run Sample Plugin")
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Text("Loading forecast...")
        }

        weather?.let {
            TrailSenseCard {
                Text("Weather forecast from plugin (placeholder in pure Compose)")
            }
        }
    }
}

class ExperimentationFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ExperimentationContent()
    }
}
