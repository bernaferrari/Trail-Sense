package com.kylecorry.trail_sense.tools.augmented_reality.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = TrailSenseSpacing.xxxl
            )
        ) {
            item("title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = TrailSenseSpacing.xl,
                            end = TrailSenseSpacing.sm,
                            top = TrailSenseSpacing.sm,
                            bottom = TrailSenseSpacing.sm
                        ),
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
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                }
            }
            item("override_reason") {
                Text(
                    text = stringResource(R.string.ar_layer_override_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = TrailSenseSpacing.xl,
                        end = TrailSenseSpacing.xl,
                        bottom = TrailSenseSpacing.md
                    )
                )
            }
            item("beacons_header") {
                TrailSenseSectionHeader(title = stringResource(R.string.beacons))
            }
            item("beacons") {
                SettingsSwitchRow(
                    label = stringResource(R.string.visible),
                    checked = showBeaconLayer,
                    onCheckedChange = onShowBeaconLayerChange,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            item("paths_header") {
                TrailSenseSectionHeader(title = stringResource(R.string.paths))
            }
            item("paths") {
                SettingsSwitchRow(
                    label = stringResource(R.string.visible),
                    checked = showPathLayer,
                    onCheckedChange = onShowPathLayerChange,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            item("astronomy_header") {
                TrailSenseSectionHeader(title = stringResource(R.string.astronomy))
            }
            item("astronomy") {
                SettingsSwitchRow(
                    label = stringResource(R.string.visible),
                    checked = showAstronomyLayer,
                    onCheckedChange = onShowAstronomyLayerChange,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            item("grid_header") {
                TrailSenseSectionHeader(title = stringResource(R.string.grid))
            }
            item("grid") {
                SettingsSwitchRow(
                    label = stringResource(R.string.visible),
                    checked = showGridLayer,
                    onCheckedChange = onShowGridLayerChange,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
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
