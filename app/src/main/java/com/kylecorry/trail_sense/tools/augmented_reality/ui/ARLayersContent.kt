package com.kylecorry.trail_sense.tools.augmented_reality.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

/**
 * The AR layers sheet: one group with a switch per layer, each named by its layer rather than a header over a
 * "Visible" switch, and the note about modes that override these choices as the group's footer.
 */
@Composable
fun ARLayersContent(
    showBeaconLayer: Boolean,
    onShowBeaconLayerChange: (Boolean) -> Unit,
    showPathLayer: Boolean,
    onShowPathLayerChange: (Boolean) -> Unit,
    showAstronomyLayer: Boolean,
    onShowAstronomyLayerChange: (Boolean) -> Unit,
    showGridLayer: Boolean,
    onShowGridLayerChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overrideMessage = stringResource(R.string.ar_layer_override_message)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            bottom = TrailSenseSpacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        item("title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.layers),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            }
        }
        trailSenseSection(
            key = "layers",
            footer = overrideMessage
        ) {
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.beacons),
                    checked = showBeaconLayer,
                    onCheckedChange = onShowBeaconLayerChange,
                    position = position
                )
            }
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.paths),
                    checked = showPathLayer,
                    onCheckedChange = onShowPathLayerChange,
                    position = position
                )
            }
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.astronomy),
                    checked = showAstronomyLayer,
                    onCheckedChange = onShowAstronomyLayerChange,
                    position = position
                )
            }
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.grid),
                    checked = showGridLayer,
                    onCheckedChange = onShowGridLayerChange,
                    position = position
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ARLayersContentPreview() {
    TrailSenseTheme {
        ARLayersContent(
            showBeaconLayer = true,
            onShowBeaconLayerChange = {},
            showPathLayer = true,
            onShowPathLayerChange = {},
            showAstronomyLayer = true,
            onShowAstronomyLayerChange = {},
            showGridLayer = true,
            onShowGridLayerChange = {},
            onClose = {}
        )
    }
}
