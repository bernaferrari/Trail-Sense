package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

@Composable
fun Fragment.WhiteNoiseContent(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
    ) {
        TrailSenseButton(
            text = if (isPlaying) "Stop" else "Play",
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.fillMaxWidth()
        )

        TrailSenseSectionHeader(title = "Sound", modifier = Modifier.padding(top = TrailSenseSpacing.md))

        var selectedSound by remember { mutableStateOf("White Noise") }
        Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
            listOf("White", "Pink", "Brown").forEach { sound ->
                TrailSenseChip(
                    text = sound,
                    selected = selectedSound == sound,
                    onClick = { selectedSound = sound }
                )
            }
        }

        TrailSenseDivider(modifier = Modifier.padding(vertical = TrailSenseSpacing.md))

        var sleepTimerEnabled by remember { mutableStateOf(false) }
        TrailSenseSwitchRow(
            label = "Sleep timer",
            checked = sleepTimerEnabled,
            onCheckedChange = { sleepTimerEnabled = it }
        )

        var durationText by remember { mutableStateOf("30") }
        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it },
            label = { Text("Duration (min)") },
            modifier = Modifier.fillMaxWidth().padding(top = TrailSenseSpacing.sm)
        )

        Spacer(Modifier.height(TrailSenseSpacing.xl))

        TrailSenseCard {
            Text(
                text = if (isPlaying) getString(R.string.on) else getString(R.string.off),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

class FragmentToolWhiteNoise : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        WhiteNoiseContent()
    }
}
