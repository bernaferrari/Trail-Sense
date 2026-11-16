@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.declination.ui

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem

import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.shared.sensors.SensorRefreshPolicy

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Help
import com.kylecorry.trail_sense.shared.icons.trail.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.drawText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.math.arithmetic.Arithmetic
import kotlin.math.abs
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

/**
 * Declination: the angle and its arrow are the hero; the place it's for is one field under it, with the GPS
 * button at its start and help at its end. "Loading" is a quiet footer, not a row of its own.
 */
@Composable
fun DeclinationScreen(
    modifier: Modifier = Modifier
) {
    val formatter = remember { FormatService.getInstance() }
    val sensors = remember { getAppService<ISensorSubsystem>() }
    var location by remember { mutableStateOf(sensors.lastKnownLocation) }
    var isLocationUpToDate by remember { mutableStateOf(false) }
    var declination by remember { mutableStateOf(0f) }
    var fromCompass by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        location = withContext(ioDispatcher) {
            sensors.getLocation(SensorRefreshPolicy.RefreshIfInvalid)
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

    // One question: how far is magnetic north from true north here, and which way?
    TrailSensePage(
        title = appString(Res.string.declination),
        onBack = rememberTrailSenseBack(),
        backLabel = appString(Res.string.back),
        modifier = modifier
    ) {
        item("declination") {
            val aligned = Arithmetic.isZero(declination, 0.05f)
            DeclinationHeroCard(
                title = title,
                verdict = if (aligned) {
                    appString(Res.string.declination_verdict_aligned)
                } else {
                    appString(
                        if (declination < 0) Res.string.declination_west_of_true else Res.string.declination_east_of_true
                    )
                },
                declination = declination,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title")
            )
        }

        // What the number is for, in plain words: where the user's bearing came from (compass or map), then one
        // sentence with the exact amount to add or subtract to get the other one.
        item("correction") {
            val east = declination >= 0
            val aligned = Arithmetic.isZero(declination, 0.05f)
            val amount = formatter.formatDegrees(abs(declination), 1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                Text(
                    text = appString(Res.string.declination_bearing_from),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TrailSenseConnectedChoice(
                    options = listOf(
                        TrailSenseChoice(true, appString(Res.string.declination_from_compass)),
                        TrailSenseChoice(false, appString(Res.string.declination_from_map))
                    ),
                    selected = fromCompass,
                    onSelect = { fromCompass = it }
                )
                Text(
                    text = when {
                        aligned -> appString(Res.string.declination_no_correction)
                        // A compass bearing is magnetic: true = magnetic + declination (east is positive).
                        fromCompass && east -> appString(Res.string.declination_compass_add, amount)
                        fromCompass -> appString(Res.string.declination_compass_subtract, amount)
                        east -> appString(Res.string.declination_map_subtract, amount)
                        else -> appString(Res.string.declination_map_add, amount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                )
            }
        }

        trailSenseSection(
            key = "location",
            title = null,
            footer = if (isLocationUpToDate) null else getAppService<AppStrings>().get(Res.string.loading)
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
 * The instrument is a compass face on the plain page: one calm disc with N, E, S and W, true north as a ghosted
 * outline needle pointing at N, and magnetic north as the one solid primary needle that swings away from it on a
 * spring when the screen opens, so the gap between the two needles is the declination. The wedge between them is
 * filled in primary. Under it, the reading (display type) and
 * the verdict (headline small).
 */
@Composable
private fun DeclinationHeroCard(
    title: String,
    verdict: String,
    declination: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = scheme.surfaceContainer
            ) {}
            DeclinationArrow(
                declination = declination,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("declination")
            )
        }
        Spacer(Modifier.size(TrailSenseSpacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = verdict,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CoordinateInput(
    location: Coordinate,
    position: TrailSenseGroupPosition,
    onLocationChanged: (Coordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { FormatService.getInstance() }
    val sensors = remember { getAppService<ISensorSubsystem>() }
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
        label = appString(Res.string.location),
        modifier = modifier.testTag("utm"),
        position = position,
        keyboardType = KeyboardType.Text,
        isError = coordinateText.isNotBlank() && parsedCoordinate == null,
        errorText = appString(Res.string.coordinate_input_invalid_location),
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
                                val updated = withContext(ioDispatcher) {
                                    sensors.getLocation(SensorRefreshPolicy.Refresh)
                                }
                                onLocationChanged(updated)
                                isLoadingLocation = false
                            }
                        },
                        modifier = Modifier.testTag("gps_btn")
                    ) {
                        Icon(
                            painter = rememberVectorPainter(TrailIcons.Location),
                            contentDescription = appString(Res.string.gps),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Help),
                    contentDescription = appString(Res.string.location_input_help_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (showHelp) {
        TrailSenseDialog(
            onDismissRequest = { showHelp = false },
            title = appString(Res.string.location_input_help_title),
            confirmText = appString(Res.string.done)
        ) {
            Text(appString(Res.string.location_input_help))
        }
    }
}

@Composable
private fun DeclinationArrow(
    declination: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trueColor = scheme.tertiary
    val magneticColor = scheme.primary
    val tick = scheme.onSurfaceVariant.copy(alpha = 0.55f)
    // The wedge between the norths is a soft wash of the needle's colour, so it never blends into the bezel.
    val wedge = scheme.primary.copy(alpha = 0.32f)
    val letterColor = scheme.onSurface
    val letters = listOf(
        appString(Res.string.direction_north),
        appString(Res.string.direction_east),
        appString(Res.string.direction_south),
        appString(Res.string.direction_west)
    )
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val letterStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
    // The magnetic needle swings in from true north with a springy overshoot.
    val needleAngle = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(declination) {
        needleAngle.animateTo(
            declination,
            androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
        )
    }
    val animatedDeclination = needleAngle.value
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val needle = radius * 0.64f // the head stays clear of the N

        // N, E, S and W just inside the rim; ticks every 15 degrees between them.
        letters.forEachIndexed { i, letter ->
            val angle = (i * 90 - 90).toDouble().toRadians()
            val r = radius - 22.dp.toPx()
            val layout = textMeasurer.measure(
                letter,
                letterStyle.copy(color = if (i == 0) letterColor else letterColor.copy(alpha = 0.7f))
            )
            drawText(
                layout,
                topLeft = Offset(
                    c.x + (kotlin.math.cos(angle) * r).toFloat() - layout.size.width / 2f,
                    c.y + (kotlin.math.sin(angle) * r).toFloat() - layout.size.height / 2f
                )
            )
        }
        for (i in 0 until 24) {
            if (i % 6 == 0) continue
            val long = false
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
        // True north: a ghosted outline needle pointing straight at N.
        val ghost = Path().apply {
            moveTo(c.x, c.y - needle - 4.dp.toPx())
            lineTo(c.x - 11.dp.toPx(), c.y - needle + 20.dp.toPx())
            lineTo(c.x + 11.dp.toPx(), c.y - needle + 20.dp.toPx())
            close()
        }
        drawLine(
            trueColor.copy(alpha = 0.6f),
            c,
            Offset(c.x, c.y - needle + 20.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawPath(ghost, trueColor.copy(alpha = 0.6f), style = Stroke(2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // Magnetic north: the one needle, in primary, with a filled head.
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
        drawCircle(scheme.surfaceContainer, radius = 4.dp.toPx(), center = c)
    }
}
