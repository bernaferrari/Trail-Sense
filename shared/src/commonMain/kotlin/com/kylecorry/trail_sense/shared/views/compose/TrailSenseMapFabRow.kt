package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R

@Composable
fun TrailSenseMapFabRow(
    onLocationClick: () -> Unit,
    onLayersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(onClick = onLocationClick) {
            Icon(painterResource(R.drawable.ic_location), contentDescription = "My location")
        }
        FloatingActionButton(onClick = onLayersClick) {
            Icon(painterResource(R.drawable.ic_layers), contentDescription = "Layers")
        }
    }
}
