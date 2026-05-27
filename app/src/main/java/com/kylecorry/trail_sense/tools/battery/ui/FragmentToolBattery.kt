package com.kylecorry.trail_sense.tools.battery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInfoRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseProgressBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

@Composable
fun Fragment.BatteryContent(modifier: Modifier = Modifier) {
    var batteryPercent by remember { mutableStateOf(85) }
    var isCharging by remember { mutableStateOf(false) }
    var timeUntilEmpty by remember { mutableStateOf("23h") }

    LaunchedEffect(Unit) {
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$batteryPercent%",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(Modifier.height(8.dp))
                TrailSenseProgressBar(progress = batteryPercent / 100f)
                Text(timeUntilEmpty, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))


        val stats = listOf(
            "Charging" to if (isCharging) "Yes" else "No",
            "Health" to "Good",
            "Temperature" to "28°C",
            "Voltage" to "4.2V"
        )

        TrailSenseCard {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                stats.forEachIndexed { index, (label, value) ->
                    TrailSenseInfoRow(label = label, value = value)
                    if (index < stats.lastIndex) {
                        TrailSenseDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))


        var showOnLock by remember { mutableStateOf(true) }
        TrailSenseSwitchRow(
            label = "Show on lock screen",
            checked = showOnLock,
            onCheckedChange = { showOnLock = it }
        )
    }
}

class FragmentToolBattery : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        BatteryContent()
    }
}
