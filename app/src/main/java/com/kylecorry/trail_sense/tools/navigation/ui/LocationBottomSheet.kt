package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class LocationSheetState(
    val location: String,
    val coordinateFormat: String,
    val accuracy: String? = null,
    val satellites: String? = null,
    val time: String? = null,
    val datum: String
)

@Composable
fun LocationSheet(
    initialFormat: BuiltInCoordinateFormat,
    getState: (BuiltInCoordinateFormat) -> LocationSheetState,
    onFormatSelected: (BuiltInCoordinateFormat) -> Unit,
    onPickFormat: (BuiltInCoordinateFormat, (BuiltInCoordinateFormat) -> Unit) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null
) {
    var selectedFormat by remember(initialFormat) { mutableStateOf(initialFormat) }
    var state by remember(selectedFormat) { mutableStateOf(getState(selectedFormat)) }

    LaunchedEffect(selectedFormat) {
        state = getState(selectedFormat)
        while (isActive) {
            delay(1000)
            state = getState(selectedFormat)
        }
    }

    LocationSheetContent(
        state = state,
        onFormatClick = {
            onPickFormat(selectedFormat) {
                selectedFormat = it
                onFormatSelected(it)
                state = getState(it)
            }
        },
        onShare = onShare,
        onCopy = onCopy?.let { copy -> { copy(state.location) } },
        modifier = modifier
    )
}

/**
 * Where you are, as a sheet: the coordinates large and left-aligned with their format right under them, three small
 * facts (accuracy, satellites, last fix) as tiles, the datum as a footnote, and the two things people do with a
 * location (copy it, share it) as full buttons. One surface, no inner background.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocationSheetContent(
    state: LocationSheetState,
    onFormatClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.location),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurfaceVariant
        )
        Text(
            text = state.location,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            color = scheme.onSurface,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("location_title")
        )
        androidx.compose.material3.AssistChip(
            onClick = onFormatClick,
            label = { Text(state.coordinateFormat) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("location_format")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.accuracy?.let { LocationFact(it, stringResource(R.string.location_accuracy_label), Modifier.weight(1f).testTag("accuracy")) }
            state.satellites?.let { LocationFact(it, stringResource(R.string.location_satellites_label), Modifier.weight(1f).testTag("satellites")) }
            state.time?.let { LocationFact(it, stringResource(R.string.location_updated_label), Modifier.weight(1f).testTag("time")) }
        }
        Text(
            text = stringResource(R.string.location_datum_value, state.datum),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 12.dp)
                .testTag("datum")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onCopy != null) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(android.R.string.copy), style = MaterialTheme.typography.titleSmall)
                }
            }
            androidx.compose.material3.Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("location_share")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_ellipsis).removeSuffix("…").removeSuffix("..."), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** One fact as a small tile: the value, and what it is under it. */
@Composable
private fun LocationFact(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSheetContentPreview() {
    TrailSenseTheme {
        LocationSheetContent(
            state = LocationSheetState(
                location = "40.7128, -74.0060",
                coordinateFormat = "Decimal degrees",
                accuracy = "± 5 m",
                satellites = "8",
                time = "1 min ago",
                datum = stringResource(R.string.wgs84)
            ),
            onFormatClick = {},
            onShare = {}
        )
    }
}
