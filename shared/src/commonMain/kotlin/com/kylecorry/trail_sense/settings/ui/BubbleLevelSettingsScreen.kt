package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlin.math.roundToInt

@Composable
fun BubbleLevelSettingsToolScreen() {
    val prefs = remember { DependencyRegistry.get<UserPreferences>().bubbleLevel }
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    var threshold by remember { mutableIntStateOf(prefs.threshold.toInt()) }
    SettingsList(title = appString(Res.string.tool_bubble_level_title)) {
        trailSenseSection("threshold") {
            row { position ->
                // Chosen in place: a whole-degree slider with its value beside the label, no dialog.
                SettingsSliderRow(
                    label = appString(Res.string.pref_bubble_level_threshold_title),
                    value = threshold.toFloat(),
                    valueRange = 0f..10f,
                    steps = 9,
                    formatValue = { formatter.formatDegrees(it.roundToInt().toFloat()) },
                    onValueChange = {
                        val degrees = it.roundToInt()
                        prefs.threshold = degrees.toFloat()
                        threshold = degrees
                    },
                    position = position
                )
            }
        }
    }
}
