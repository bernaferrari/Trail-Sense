@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
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
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer
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
                // Unset: a small empty compass with a dashed ring, waiting for a direction. Set: the arrow cookie.
                if (isSet) {
                    PlacesBadge(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        icon = TrailIcons.NavigationArrow,
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        size = 52.dp,
                        iconRotation = bearing ?: 0f
                    )
                } else {
                    PlacesEmptyDial(size = 52.dp)
                }
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

            // Taking the bearing from the compass is the field's one main action (filled); the camera and typing it
            // in are quieter icon buttons beside it. Without a compass, typing it in is the one button.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCompass) {
                    val compassLabel = stringResource(R.string.places_triangulate_compass)
                    Box(Modifier.weight(1f).testTag("compass_btn")) {
                        TrailSenseButton(
                            text = compassBearingText,
                            onClick = onCompassClick,
                            icon = TrailIcons.CompassIcon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "$compassLabel $compassBearingText" }
                                .testTag("compass_bearing")
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("camera_btn")
                    ) {
                        Icon(TrailIcons.Camera, contentDescription = stringResource(R.string.camera))
                    }
                    FilledTonalIconButton(
                        onClick = { showManualEntry = true },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("manual_bearing")
                    ) {
                        Icon(TrailIcons.Edit, contentDescription = stringResource(R.string.enter_manually))
                    }
                } else {
                    TrailSenseButton(
                        text = stringResource(R.string.enter_manually),
                        onClick = { showManualEntry = true },
                        icon = TrailIcons.Edit,
                        style = TrailSenseButtonStyle.Tonal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_bearing")
                    )
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
internal fun PlacesBearingDialog(
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

/** A compass with no direction yet: a tinted disc, a dashed ring and a dot where the needle will pivot. */
@Composable
fun PlacesEmptyDial(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val disc = MaterialTheme.colorScheme.surfaceContainerHighest
    val ring = MaterialTheme.colorScheme.outline
    val north = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val radius = this.size.minDimension / 2
        drawCircle(disc, radius = radius)
        drawCircle(
            ring,
            radius = radius - 6.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                )
            )
        )
        // North tick, so it still reads as a compass.
        drawLine(
            north,
            androidx.compose.ui.geometry.Offset(center.x, 3.dp.toPx()),
            androidx.compose.ui.geometry.Offset(center.x, 10.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(ring, radius = 3.dp.toPx())
    }
}
