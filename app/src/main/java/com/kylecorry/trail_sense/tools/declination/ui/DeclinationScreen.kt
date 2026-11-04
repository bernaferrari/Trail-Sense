@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.declination.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Help
import com.kylecorry.trail_sense.shared.icons.trail.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Declination: the angle and its arrow are the hero; the place it's for is one field under it, with the GPS
 * button at its start and help at its end. "Loading" is a quiet footer, not a row of its own.
 */
@Composable
fun DeclinationScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    var location by remember { mutableStateOf(sensors.lastKnownLocation) }
    var isLocationUpToDate by remember { mutableStateOf(false) }
    var declination by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        location = withContext(Dispatchers.IO) {
            sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.RefreshIfInvalid)
        }
        isLocationUpToDate = true
    }

    LaunchedEffect(location) {
        declination = withContext(Dispatchers.Default) {
            Geophysics.getGeomagneticDeclination(location)
        }
    }

    val title = remember(declination, formatter) {
        if (Arithmetic.isZero(declination, 0.05f)) {
            formatter.formatDegrees(declination, 1)
        } else {
            "${formatter.formatDegrees(declination, 1)} (${
                formatter.formatDirection(
                    if (declination < 0) {
                        CompassDirection.West
                    } else {
                        CompassDirection.East
                    }
                )
            })"
        }
    }

    TrailSensePage(
        title = stringResource(R.string.declination),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item("declination") {
            DeclinationHeroCard(
                title = title,
                declination = declination,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title")
            )
        }

        item("stats") {
            val aligned = Arithmetic.isZero(declination, 0.05f)
            TrailSenseStatGrid(
                stats = listOf(
                    TrailSenseStat(
                        label = stringResource(R.string.declination),
                        value = formatter.formatDegrees(kotlin.math.abs(declination), 1),
                        supportingText = stringResource(R.string.places_declination_here),
                        tone = TrailSenseTone.Primary,
                        icon = TrailIcons.Declination
                    ),
                    TrailSenseStat(
                        label = stringResource(R.string.magnetic_north),
                        value = if (aligned) {
                            stringResource(R.string.places_declination_aligned)
                        } else {
                            formatter.formatDirection(if (declination < 0) CompassDirection.West else CompassDirection.East)
                        },
                        supportingText = stringResource(
                            if (aligned) R.string.places_declination_with_true_north else R.string.places_declination_of_true_north
                        ),
                        tone = TrailSenseTone.Tertiary,
                        icon = TrailIcons.CompassIcon
                    )
                )
            )
        }

        trailSenseSection(
            key = "location",
            title = null,
            footer = if (isLocationUpToDate) null else context.getString(R.string.loading)
        ) {
            row { position ->
                CoordinateInput(
                    location = location,
                    position = position,
                    onLocationChanged = {
                        location = it
                        isLocationUpToDate = true
                    },
                    modifier = Modifier.testTag("location")
                )
            }
        }
    }
}

/**
 * The hero is a compass face: a saturated twelve-sided cookie as the bezel, a calm dial inside, true north as a
 * star-tipped needle and magnetic north as a tertiary needle, with the angle between them filled in.
 */
@Composable
private fun DeclinationHeroCard(
    title: String,
    declination: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(248.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = scheme.primary
                ) {}
                Surface(
                    modifier = Modifier.size(204.dp),
                    shape = CircleShape,
                    color = scheme.surfaceContainerLowest
                ) {}
                DeclinationArrow(
                    declination = declination,
                    modifier = Modifier
                        .size(204.dp)
                        .testTag("declination")
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CoordinateInput(
    location: Coordinate,
    position: TrailSenseGroupPosition,
    onLocationChanged: (Coordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val scope = rememberCoroutineScope()
    var coordinateText by remember(location, formatter) { mutableStateOf(formatter.formatLocation(location)) }
    val parsedCoordinate = remember(coordinateText) { Coordinate.parse(coordinateText) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(location) {
        val formatted = formatter.formatLocation(location)
        if (coordinateText != formatted) {
            coordinateText = formatted
        }
    }

    TrailSenseInputField(
        value = coordinateText,
        onValueChange = {
            coordinateText = it
            Coordinate.parse(it)?.let(onLocationChanged)
        },
        label = stringResource(R.string.location),
        modifier = modifier.testTag("utm"),
        position = position,
        keyboardType = KeyboardType.Text,
        isError = coordinateText.isNotBlank() && parsedCoordinate == null,
        errorText = stringResource(R.string.coordinate_input_invalid_location),
        enabled = !isLoadingLocation,
        leadingIcon = {
            Box(
                modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("gps_loading"),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoadingLocation = true
                                val updated = withContext(Dispatchers.IO) {
                                    sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
                                }
                                onLocationChanged(updated)
                                isLoadingLocation = false
                            }
                        },
                        modifier = Modifier.testTag("gps_btn")
                    ) {
                        Icon(
                            painter = rememberVectorPainter(TrailIcons.Location),
                            contentDescription = stringResource(R.string.gps),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        trailingIcon = {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Help),
                    contentDescription = stringResource(R.string.location_input_help_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.location_input_help_title)) },
            text = { Text(stringResource(R.string.location_input_help)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun DeclinationArrow(
    declination: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trueColor = scheme.onSurface
    val magneticColor = scheme.tertiary
    val tick = scheme.outlineVariant
    val wedge = scheme.tertiaryContainer
    val animatedDeclination by animateFloatAsState(
        targetValue = declination,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "declinationRotation"
    )
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val needle = radius * 0.72f

        // Ticks every 15 degrees, longer on the quarters.
        for (i in 0 until 24) {
            val long = i % 6 == 0
            rotate(i * 15f, c) {
                drawLine(
                    tick,
                    Offset(c.x, c.y - radius + 10.dp.toPx()),
                    Offset(c.x, c.y - radius + (if (long) 22.dp else 15.dp).toPx()),
                    strokeWidth = (if (long) 3.dp else 2.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // The angle between the two norths, at least a sliver so it can be seen.
        val sweep = if (kotlin.math.abs(animatedDeclination) < 1f) {
            if (animatedDeclination < 0) -1f else 1f
        } else {
            animatedDeclination
        }
        drawArc(
            color = wedge,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = Offset(c.x - needle, c.y - needle),
            size = androidx.compose.ui.geometry.Size(needle * 2, needle * 2)
        )

        // True north: a needle up with a filled star at its tip.
        drawLine(trueColor, c, Offset(c.x, c.y - needle + 14.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawStar(trueColor, Offset(c.x, c.y - needle), 26.dp.toPx())

        // Magnetic north: a tapered tertiary needle with a filled head.
        rotate(animatedDeclination, c) {
            drawLine(magneticColor, c, Offset(c.x, c.y - needle + 18.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(c.x, c.y - needle - 4.dp.toPx())
                lineTo(c.x - 11.dp.toPx(), c.y - needle + 20.dp.toPx())
                lineTo(c.x + 11.dp.toPx(), c.y - needle + 20.dp.toPx())
                close()
            }
            drawPath(head, magneticColor)
        }
        drawCircle(scheme.primary, radius = 9.dp.toPx(), center = c)
        drawCircle(scheme.surfaceContainerLowest, radius = 4.dp.toPx(), center = c)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    color: Color,
    center: Offset,
    size: Float
) {
    val radius = size / 2f
    val path = Path()
    val points = 10
    for (i in 0 until points) {
        val angle = -Math.PI / 2 + i * Math.PI / 5
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val point = Offset(
            x = center.x + (kotlin.math.cos(angle) * r).toFloat(),
            y = center.y + (kotlin.math.sin(angle) * r).toFloat()
        )
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    path.close()
    drawPath(path, color)
}
