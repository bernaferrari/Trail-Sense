package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun TrailSenseMapFabRow(
    onLocationClick: () -> Unit,
    onLayersClick: () -> Unit,
    locationIcon: Painter,
    layersIcon: Painter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(onClick = onLocationClick) {
            Icon(locationIcon, contentDescription = "My location")
        }
        FloatingActionButton(onClick = onLayersClick) {
            Icon(layersIcon, contentDescription = "Layers")
        }
    }
}
