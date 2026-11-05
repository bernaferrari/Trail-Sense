@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.solarpanel.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.icons.trail.Sun
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Arrow
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.Edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.IDeclinationStrategy
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.tools.solarpanel.domain.SolarPanelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.math.absoluteValue

/**
 * The solar panel aligner as a page: when to charge is a segmented choice at the top, then one card per axis with
 * the target as the hero number, where the phone is now under it, and an arrow beside it saying which way to move
 * (the cue sits next to the number it fixes instead of at the screen's edges). A card turns to the confirmation
 * tone once that axis is aligned. The energy estimate closes the page.
 */
@Composable
fun SolarPanelScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val solarPanelService = remember { SolarPanelService() }
    val sensorService = remember { SensorService(context) }
    val gps = remember { sensorService.getGPS() }
    val compass = remember { sensorService.getCompass() }
    val orientation = remember { Level(sensorService.getOrientation()) }
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val declination = remember { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    var position by remember { mutableStateOf<Pair<Float, Bearing>?>(null) }
    var nowDuration by remember { mutableStateOf(Duration.ofHours(2)) }
    var alignToRestOfDay by remember { mutableStateOf(true) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var gpsUpdateKey by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner, compass, orientation, gps, view) {
        var isStarted = false
        val sensorListener = { true }
        val gpsListener = {
            gpsUpdateKey++
            false
        }

        fun startSensors() {
            if (isStarted) {
                return
            }
            isStarted = true
            view.keepScreenOn = true
            compass.start(sensorListener)
            orientation.start(sensorListener)
            gps.start(gpsListener)
        }

        fun stopSensors() {
            if (!isStarted) {
                return
            }
            isStarted = false
            compass.stop(sensorListener)
            orientation.stop(sensorListener)
            gps.stop(gpsListener)
            view.keepScreenOn = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    startSensors()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                    stopSensors()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (isResumed) {
            startSensors()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopSensors()
        }
    }

    LaunchedEffect(isResumed) {
        if (!isResumed) {
            return@LaunchedEffect
        }
        while (isActive) {
            frame++
            delay(33)
        }
    }
    val durationOfChargeTitle = stringResource(R.string.duration_of_charge)

    LaunchedEffect(alignToRestOfDay, nowDuration, gpsUpdateKey) {
        position = null
        val duration = if (alignToRestOfDay) Duration.ofDays(1) else nowDuration
        position = withContext(Dispatchers.IO) {
            solarPanelService.getBestPosition(
                gps.location,
                duration,
                restrictToToday = alignToRestOfDay
            )
        }
    }

    val modeOptions = listOf(
        TrailSenseChoice(true, stringResource(R.string.today)),
        TrailSenseChoice(false, durationOfChargeTitle)
    )

    // One question: which way should the panel point?
    TrailSensePage(
        title = stringResource(R.string.tool_solar_panel_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        val currentPosition = position
        if (currentPosition == null) {
            item("loading") {
                // Holds roughly the hero's height so nothing jumps when the position arrives.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(72.dp).testTag("solar_loading"))
                }
            }
        } else {
            item("energy") {
                frame.hashCode()
                SolarHero(
                    energy = getEnergyText(
                        solarPanelService,
                        gps,
                        orientation,
                        compass,
                        alignToRestOfDay,
                        nowDuration,
                        formatter,
                        context.getString(R.string.up_to_amount),
                        context.getString(R.string.solar_no_sun_left)
                    ),
                    position = currentPosition,
                    compass = compass,
                    orientation = orientation,
                    declination = declination,
                    prefs = prefs,
                    formatter = formatter
                )
            }
        }

        item("mode") {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                TrailSenseConnectedChoice(
                    options = modeOptions,
                    selected = alignToRestOfDay,
                    onSelect = { today ->
                        if (today) {
                            alignToRestOfDay = true
                        } else {
                            // The window switches only once a duration is picked, as before.
                            showDurationPicker = true
                        }
                    },
                    modifier = Modifier.testTag(if (alignToRestOfDay) "solar_today_btn" else "solar_now_btn")
                )
                AnimatedVisibility(
                    visible = !alignToRestOfDay,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TrailSenseListItem(
                        title = formatter.formatDuration(nowDuration),
                        subtitle = durationOfChargeTitle,
                        position = TrailSenseGroupPosition.Single,
                        onClick = { showDurationPicker = true },
                        modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                        trailing = {
                            Icon(
                                painter = rememberVectorPainter(TrailIcons.Edit),
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        if (currentPosition != null) {
            item("readout") {
                SolarPanelReadout(
                    position = currentPosition,
                    compass = compass,
                    orientation = orientation,
                    formatter = formatter,
                    prefs = prefs,
                    declination = declination,
                    frame = frame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.sm)
                        .testTag("solar_content")
                )
            }
        }
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            title = durationOfChargeTitle,
            duration = nowDuration,
            onDurationPicked = {
                if (it != null) {
                    nowDuration = it
                    alignToRestOfDay = false
                }
                showDurationPicker = false
            },
            onDismiss = {
                showDurationPicker = false
            }
        )
    }
}

/**
 * The instrument: a side view of the panel (the dashed target and the live panel tilting toward it, with the sun
 * beyond) on the plain page. Under it, the verdict is the next thing to do, and the energy it earns follows.
 */
@Composable
private fun SolarHero(
    energy: String,
    position: Pair<Float, Bearing>,
    compass: ICompass,
    orientation: Level,
    declination: IDeclinationStrategy,
    prefs: UserPreferences,
    formatter: FormatService
) {
    val altitudeDiff = getAltitudeDiff(position, orientation)
    val azimuthDiff = getAzimuthDiff(position, compass, declination, prefs)
    val tiltDone = altitudeDiff.absoluteValue < ALTITUDE_THRESHOLD
    val turnDone = azimuthDiff.absoluteValue < AZIMUTH_THRESHOLD
    val aligned = tiltDone && turnDone
    val status = trailSenseStatusColors()
    val scheme = MaterialTheme.colorScheme
    val panelColor by animateColorAsState(
        if (aligned) status.good else scheme.primary,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "panel"
    )
    // The verdict says what is left to do, one axis at a time.
    val verdict = when {
        aligned -> stringResource(R.string.solar_aligned)
        !tiltDone -> stringResource(
            if (altitudeDiff > 0) R.string.solar_tilt_up else R.string.solar_tilt_down,
            formatter.formatDegrees(altitudeDiff.absoluteValue)
        )
        else -> stringResource(
            if (azimuthDiff < 0) R.string.solar_turn_left else R.string.solar_turn_right,
            formatter.formatDegrees(azimuthDiff.absoluteValue)
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        // The one instruction, on the page instead of in a dialog that hid the panel on every visit.
        Text(
            text = stringResource(R.string.solar_panel_instructions),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PanelSideView(
            currentTilt = orientation.y,
            targetTilt = position.first,
            aligned = aligned,
            color = panelColor,
            ink = scheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Text(
            text = verdict,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = if (aligned) status.good else scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = energy,
            modifier = Modifier.testTag("energy"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The side view: a ground line, the target as a dashed outline of the panel, the live panel as a solid rounded bar on its post,
 * and the sun as a scalloped badge out along the target panel's face with a dashed ray down to it.
 */
@Composable
private fun PanelSideView(
    currentTilt: Float,
    targetTilt: Float,
    aligned: Boolean,
    color: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    val tilt by animateFloatAsState(currentTilt.coerceIn(-10f, 100f), TrailSenseAnimation.standardSpring, label = "tilt")
    val target = targetTilt.coerceIn(0f, 90f)
    // The sun's rays settle into a round disc as the panel lines up with it (shape is the state).
    val sunMorph = remember { Morph(MaterialShapes.Sunny, MaterialShapes.Circle) }
    val roundness by animateFloatAsState(
        if (aligned) 1f else 0f,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "sunRound"
    )
    val sunColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.clipToBounds()) {
        val ground = size.height - 10.dp.toPx()
        // Half a panel, sized so a panel stood on end still clears the ground below and the top above.
        val half = (size.width * 0.21f).coerceAtMost((ground - 28.dp.toPx()) / 2f)
        val length = half * 2f
        val pivot = Offset(size.width * 0.34f, ground - half - 14.dp.toPx())
        // Ground.
        drawLine(ink.copy(alpha = 0.3f), Offset(0f, ground), Offset(size.width, ground), 3.dp.toPx(), StrokeCap.Round)

        fun ends(angle: Float): Pair<Offset, Offset> {
            // A panel tilted by `angle` from flat rises to the left of the pivot, facing the sun up to the right.
            val r = Math.toRadians(angle.toDouble())
            val dx = kotlin.math.cos(r).toFloat() * length / 2f
            val dy = kotlin.math.sin(r).toFloat() * length / 2f
            return Offset(pivot.x - dx, pivot.y - dy) to Offset(pivot.x + dx, pivot.y + dy)
        }

        // The sun, along the target panel's normal.
        val normal = Math.toRadians((target - 90f).toDouble())
        val reach = (size.height * 0.95f).coerceAtMost(size.width * 0.5f)
        val sun = Offset(
            (pivot.x + kotlin.math.cos(normal).toFloat() * reach).coerceIn(28.dp.toPx(), size.width - 28.dp.toPx()),
            (pivot.y + kotlin.math.sin(normal).toFloat() * reach).coerceAtLeast(28.dp.toPx())
        )
        drawLine(
            sunColor.copy(alpha = 0.7f),
            sun,
            pivot,
            3.dp.toPx(),
            StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx()))
        )
        val sunSize = 44.dp.toPx()
        val path = sunMorph.toPath(roundness).apply {
            transform(android.graphics.Matrix().apply {
                setScale(sunSize, sunSize)
                postTranslate(sun.x - sunSize / 2f, sun.y - sunSize / 2f)
            })
        }
        drawPath(path.asComposePath(), sunColor)

        // The target as a dashed outline of the panel, then the post and the live panel.
        val thickness = 14.dp.toPx()
        rotate(target, pivot) {
            drawRoundRect(
                color = ink.copy(alpha = 0.6f),
                topLeft = Offset(pivot.x - half, pivot.y - thickness / 2f),
                size = Size(length, thickness),
                cornerRadius = CornerRadius(thickness / 2f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                )
            )
        }
        drawLine(ink.copy(alpha = 0.7f), pivot, Offset(pivot.x, ground), 5.dp.toPx(), StrokeCap.Round)
        val (cs, ce) = ends(tilt)
        // Aligned, the panel's face catches the light: a soft warm glow around it.
        if (roundness > 0f) {
            drawLine(sunColor.copy(alpha = 0.35f * roundness), cs, ce, thickness * 2.4f, StrokeCap.Round)
        }
        drawLine(color, cs, ce, thickness, StrokeCap.Round)
        drawCircle(ink, 6.dp.toPx(), pivot)
    }
}

@Composable
private fun SolarPanelReadout(
    position: Pair<Float, Bearing>,
    compass: ICompass,
    orientation: Level,
    formatter: FormatService,
    prefs: UserPreferences,
    declination: IDeclinationStrategy,
    frame: Int,
    modifier: Modifier = Modifier
) {
    // Force this composable to refresh from live sensors.
    frame.hashCode()

    if (prefs.compass.useTrueNorth) {
        compass.declination = declination.getDeclination()
    } else {
        compass.declination = 0f
    }

    val declinationOffset = if (prefs.compass.useTrueNorth) {
        0f
    } else {
        -declination.getDeclination()
    }
    val desiredAzimuth = position.second.withDeclination(declinationOffset).inverse()
    val azimuthDiff = getAzimuthDiff(position, compass, declination, prefs)
    val altitudeDiff = getAltitudeDiff(position, orientation)
    val azimuthAligned = azimuthDiff.absoluteValue < AZIMUTH_THRESHOLD
    val altitudeAligned = altitudeDiff.absoluteValue < ALTITUDE_THRESHOLD

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        SolarReadoutCard(
            label = stringResource(R.string.tilt),
            labelTag = "tilt_label",
            current = formatter.formatDegrees(orientation.y),
            currentTag = "current_altitude",
            desired = formatter.formatDegrees(position.first),
            desiredTag = "desired_altitude",
            isComplete = altitudeAligned,
            completeTag = "altitude_complete",
            // Up when the panel needs to tilt up, down when it needs to tilt down.
            arrowRotation = when {
                altitudeDiff > ALTITUDE_THRESHOLD -> 0f
                altitudeDiff < -ALTITUDE_THRESHOLD -> 180f
                else -> null
            },
            arrowTag = if (altitudeDiff > 0) "arrow_up" else "arrow_down",
            closeness = 1f - (altitudeDiff.absoluteValue / 90f),
            shape = MaterialShapes.Cookie9Sided,
            position = TrailSenseGroupPosition(isFirst = true, isLast = false)
        )
        SolarReadoutCard(
            label = stringResource(R.string.compass_azimuth),
            labelTag = "azimuth_label",
            current = formatter.formatDegrees(compass.rawBearing, replace360 = true),
            currentTag = "current_azimuth",
            desired = formatter.formatDegrees(desiredAzimuth.value, replace360 = true),
            desiredTag = "desired_azimuth",
            isComplete = azimuthAligned,
            completeTag = "azimuth_complete",
            arrowRotation = when {
                azimuthDiff < -AZIMUTH_THRESHOLD -> 270f
                azimuthDiff > AZIMUTH_THRESHOLD -> 90f
                else -> null
            },
            arrowTag = if (azimuthDiff < 0) "arrow_left" else "arrow_right",
            closeness = 1f - (azimuthDiff.absoluteValue / 180f),
            shape = MaterialShapes.Cookie6Sided,
            position = TrailSenseGroupPosition(isFirst = false, isLast = true)
        )
    }
}

/**
 * One axis as a quiet row: its name, the target and where the phone is now, and a small arrow pointing the way
 * (a check once aligned). What to do is said once, in the headline above.
 */
@Composable
private fun SolarReadoutCard(
    label: String,
    labelTag: String,
    current: String,
    currentTag: String,
    desired: String,
    desiredTag: String,
    isComplete: Boolean,
    completeTag: String,
    arrowRotation: Float?,
    arrowTag: String,
    @Suppress("UNUSED_PARAMETER") closeness: Float,
    shape: androidx.graphics.shapes.RoundedPolygon,
    position: TrailSenseGroupPosition
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = scheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                // The move itself is the headline above; the row only names the axis and its two angles.
                Text(
                    text = label,
                    modifier = Modifier.testTag(labelTag),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                    Text(
                        text = stringResource(R.string.solar_target, desired),
                        modifier = Modifier.testTag(desiredTag),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.solar_now, current),
                        modifier = Modifier.testTag(currentTag),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            SolarCue(
                isComplete = isComplete,
                completeTag = completeTag,
                arrowRotation = arrowRotation,
                arrowTag = arrowTag,
                shape = shape
            )
        }
    }
}

/** A check when aligned; otherwise the way to move, or nothing while within a step of it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SolarCue(
    isComplete: Boolean,
    completeTag: String,
    arrowRotation: Float?,
    arrowTag: String,
    @Suppress("UNUSED_PARAMETER") shape: androidx.graphics.shapes.RoundedPolygon
) {
    val status = trailSenseStatusColors()
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        when {
            isComplete -> TrailSenseShapeBadge(
                icon = TrailIcons.Check,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 44.dp,
                containerColor = status.goodContainer,
                contentColor = status.onGoodContainer,
                modifier = Modifier.testTag(completeTag)
            )

            arrowRotation != null -> TrailSenseShapeBadge(
                icon = TrailIcons.Arrow,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 44.dp,
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                iconRotation = arrowRotation,
                modifier = Modifier.testTag(arrowTag)
            )
        }
    }
}

private fun getEnergyText(
    solarPanelService: SolarPanelService,
    gps: IGPS,
    orientation: Level,
    compass: ICompass,
    alignToRestOfDay: Boolean,
    nowDuration: Duration,
    formatter: FormatService,
    upToFormat: String,
    noSunText: String
): String {
    val energy = solarPanelService.getSolarEnergy(
        gps.location,
        orientation.y,
        compass.bearing.inverse(),
        if (alignToRestOfDay) Duration.ofDays(1) else nowDuration,
        restrictToToday = alignToRestOfDay
    )
    // Rounds to 0.0 (night, or the sun is behind the panel): say so in words, not "Up to 0 kWh / m²".
    if (energy < 0.05f) return noSunText
    return upToFormat.format(formatter.formatSolarEnergy(energy))
}

private fun getAzimuthDiff(
    position: Pair<Float, Bearing>?,
    compass: ICompass,
    declination: IDeclinationStrategy,
    prefs: UserPreferences
): Float {
    position ?: return 0f
    val declinationOffset = if (prefs.compass.useTrueNorth) {
        0f
    } else {
        -declination.getDeclination()
    }
    val desiredAzimuth = position.second.withDeclination(declinationOffset).inverse()
    return deltaAngle(desiredAzimuth.value, compass.rawBearing)
}

private fun getAltitudeDiff(
    position: Pair<Float, Bearing>?,
    orientation: Level
): Float {
    position ?: return 0f
    return position.first - orientation.y
}

private const val AZIMUTH_THRESHOLD = 5
private const val ALTITUDE_THRESHOLD = 5
