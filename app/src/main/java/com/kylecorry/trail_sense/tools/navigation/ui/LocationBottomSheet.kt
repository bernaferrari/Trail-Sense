package com.kylecorry.trail_sense.tools.navigation.ui

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
    modifier: Modifier = Modifier
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
        modifier = modifier
    )
}

@Composable
private fun LocationSheetContent(
    state: LocationSheetState,
    onFormatClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.location,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("location_title")
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 48.dp)
                            .clickable(onClick = onFormatClick)
                            .testTag("location_format"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.coordinateFormat,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("location_share")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_ellipsis),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LocationStats(state)

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("datum"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_datum),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.datum,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LocationStats(state: LocationSheetState) {
    val stats = listOfNotNull(
        state.accuracy?.let { "accuracy" to it },
        state.satellites?.let { "satellites" to it },
        state.time?.let { "time" to it }
    )

    if (stats.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEach { (tag, text) ->
            LocationStat(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .testTag(tag)
            )
        }
    }
}

@Composable
private fun LocationStat(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun LocationSheetContentPreview() {
    TrailSenseTheme {
        LocationSheetContent(
            state = LocationSheetState(
                location = "40.7128, -74.0060",
                coordinateFormat = "Decimal degrees",
                accuracy = "5 m accuracy",
                satellites = "8 satellites",
                time = "1 min ago",
                datum = stringResource(R.string.wgs84)
            ),
            onFormatClick = {},
            onShare = {}
        )
    }
}
