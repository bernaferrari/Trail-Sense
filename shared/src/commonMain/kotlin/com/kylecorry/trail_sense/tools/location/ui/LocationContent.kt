package com.kylecorry.trail_sense.tools.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.device.LocationReading
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.coordinates_tab
import com.kylecorry.trail_sense.shared.generated.resources.location_accuracy_label
import com.kylecorry.trail_sense.shared.generated.resources.location_not_set
import com.kylecorry.trail_sense.shared.generated.resources.my_location
import com.kylecorry.trail_sense.shared.text.appString
import kotlin.math.roundToInt

/** Displays a platform location reading; the host owns permission and sensor collection. */
@Composable
fun LocationContent(reading: LocationReading?, unavailable: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(appString(Res.string.my_location), style = MaterialTheme.typography.headlineLarge)
        when {
            reading != null -> {
                Text(appString(Res.string.coordinates_tab), style = MaterialTheme.typography.labelLarge)
                Text("${reading.coordinate.latitude}, ${reading.coordinate.longitude}", style = MaterialTheme.typography.bodyLarge)
                reading.horizontalAccuracyMeters?.let {
                    Text("${appString(Res.string.location_accuracy_label)}: ±${it.roundToInt()} m")
                }
            }
            unavailable -> Text(appString(Res.string.location_not_set))
            else -> CircularProgressIndicator()
        }
    }
}
