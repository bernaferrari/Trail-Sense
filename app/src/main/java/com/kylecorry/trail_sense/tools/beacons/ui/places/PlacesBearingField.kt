@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.icons.trail.Camera
import com.kylecorry.trail_sense.shared.icons.trail.Cancel
import com.kylecorry.trail_sense.shared.icons.trail.CompassIcon
import com.kylecorry.trail_sense.shared.icons.trail.Edit
import com.kylecorry.trail_sense.shared.icons.trail.NavigationArrow
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape

/**
 * A bearing as one row of a group: the direction in a cookie whose arrow points that way, the value large with its
 * north reference under it, and the ways to set it as one connected button row (from the compass, through the
 * camera, or typed in). Same tags as the shared bearing input so tests and automation keep working.
 */
@Composable
fun PlacesBearingField(
    bearing: Float?,
    trueNorth: Boolean,
    compassBearingText: String,
    hasCompass: Boolean,
    onBearingChange: (Float?, Boolean) -> Unit,
    onCompassClick: () -> Unit,
    onCameraClick: () -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    tag: String? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    var showManualEntry by remember { mutableStateOf(false) }
    val isSet = bearing != null
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(tag?.let { Modifier.testTag(it) } ?: Modifier),
        shape = trailSenseGroupShape(position),
        color = containerColor
    ) {
        Column(
            Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlacesBadge(
                    containerColor = if (isSet) scheme.primary else scheme.surfaceContainerHighest,
                    contentColor = if (isSet) scheme.onPrimary else scheme.onSurfaceVariant,
                    icon = TrailIcons.NavigationArrow,
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    size = 52.dp,
                    iconRotation = bearing ?: 0f
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.bearing),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = bearing?.let { formatter.formatDegrees(it, replace360 = true) }
                            ?: stringResource(R.string.direction_not_set),
                        modifier = Modifier.testTag("bearing"),
                        style = if (isSet) {
                            MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum")
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
                        color = scheme.onSurface
                    )
                    if (bearing != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("north_reference_badge")
                        ) {
                            Icon(
                                formatter.getCompassReferenceIcon(trueNorth),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = scheme.primary
                            )
                            Spacer(Modifier.width(TrailSenseSpacing.xs))
                            Text(
                                stringResource(if (trueNorth) R.string.true_north else R.string.magnetic_north),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (bearing != null) {
                    IconButton(
                        onClick = { onBearingChange(null, false) },
                        modifier = Modifier.testTag("clear_btn")
                    ) {
                        Icon(
                            TrailIcons.Cancel,
                            contentDescription = stringResource(android.R.string.cancel),
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val buttons = buildList {
                    if (hasCompass) add(0)
                    if (hasCompass) add(1)
                    add(2)
                }
                buttons.forEachIndexed { index, kind ->
                    val outer = 24.dp
                    val inner = 8.dp
                    val first = index == 0
                    val last = index == buttons.lastIndex
                    val shape = RoundedCornerShape(
                        topStart = if (first) outer else inner,
                        bottomStart = if (first) outer else inner,
                        topEnd = if (last) outer else inner,
                        bottomEnd = if (last) outer else inner
                    )
                    val weight = if (kind == 0) 2.2f else 0.8f
                    when (kind) {
                        0 -> Button(
                            onClick = onCompassClick,
                            shape = shape,
                            modifier = Modifier
                                .weight(weight)
                                .heightIn(min = 48.dp)
                                .testTag("compass_btn"),
                            contentPadding = PaddingValues(horizontal = TrailSenseSpacing.md)
                        ) {
                            Icon(TrailIcons.CompassIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(TrailSenseSpacing.sm))
                            Text(
                                compassBearingText,
                                modifier = Modifier.testTag("compass_bearing"),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1
                            )
                        }

                        1 -> FilledTonalButton(
                            onClick = onCameraClick,
                            shape = shape,
                            modifier = Modifier
                                .weight(weight)
                                .heightIn(min = 48.dp)
                                .testTag("camera_btn"),
                            contentPadding = PaddingValues(horizontal = TrailSenseSpacing.sm)
                        ) {
                            Icon(
                                TrailIcons.Camera,
                                contentDescription = stringResource(R.string.camera),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        else -> FilledTonalButton(
                            onClick = { showManualEntry = true },
                            shape = shape,
                            modifier = Modifier
                                .weight(weight)
                                .heightIn(min = 48.dp)
                                .testTag("manual_bearing"),
                            contentPadding = PaddingValues(horizontal = TrailSenseSpacing.sm)
                        ) {
                            Icon(
                                TrailIcons.Edit,
                                contentDescription = stringResource(R.string.enter_manually),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showManualEntry) {
        PlacesBearingDialog(
            initialBearing = bearing,
            initialTrueNorth = trueNorth,
            onDismiss = { showManualEntry = false },
            onConfirm = { selectedBearing, selectedTrueNorth ->
                showManualEntry = false
                onBearingChange(selectedBearing, selectedTrueNorth)
            }
        )
    }
}

@Composable
private fun PlacesBearingDialog(
    initialBearing: Float?,
    initialTrueNorth: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float?, Boolean) -> Unit
) {
    var bearingText by remember(initialBearing) {
        mutableStateOf(initialBearing?.let { DecimalFormatter.format(it, 1) } ?: "")
    }
    var trueNorth by remember(initialTrueNorth) { mutableStateOf(initialTrueNorth) }
    val parsedBearing = bearingText.toFloatOrNull()?.let { Bearing.getBearing(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.direction)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                TrailSenseInputField(
                    value = bearingText,
                    onValueChange = { bearingText = it },
                    label = stringResource(R.string.bearing),
                    keyboardType = KeyboardType.Number,
                    unit = "°",
                    position = TrailSenseGroupPosition(isFirst = true, isLast = false),
                    modifier = Modifier.testTag("bearing")
                )
                Surface(
                    shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = false, isLast = true)),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = TrailSenseSpacing.lg)
                            .testTag("true_north"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.true_north),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(checked = trueNorth, onCheckedChange = { trueNorth = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(parsedBearing, trueNorth) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
