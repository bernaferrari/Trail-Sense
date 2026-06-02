package com.kylecorry.trail_sense.tools.beacons.ui.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Pure Compose Beacons List screen.
 * Full list + search + groups using design system in progress.
 */
@Composable
fun BeaconsListScreen(modifier: Modifier = Modifier) {
    TrailSenseCard(modifier = modifier.padding(TrailSenseSpacing.md)) {
        Text(
            "Beacons List (pure Compose + design system)",
            modifier = Modifier.padding(TrailSenseSpacing.md)
        )
    }
}
