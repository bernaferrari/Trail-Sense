@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.solarpanel.ui

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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
import com.kylecorry.trail_sense.shared.dialogs.Alerts
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

    LaunchedEffect(Unit) {
        Alerts.dialog(
            context,
            context.getString(R.string.tool_solar_panel_title),
            context.getString(R.string.solar_panel_instructions),
            cancelText = null
        )
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

    TrailSensePage(
        title = stringResource(R.string.tool_solar_panel_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
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
                        position = TrailSenseGroupPosition.Single,
                        onClick = { showDurationPicker = true },
                        modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                        trailing = {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        val currentPosition = position
        if (currentPosition == null) {
            item("loading") {
                // Holds roughly the cards' height so nothing jumps when the position arrives.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("solar_loading"))
                }
            }
        } else {
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
                        .testTag("solar_content")
                )
            }
            item("energy") {
                TrailSenseGroupCard(position = TrailSenseGroupPosition.Single) {
                    Text(
                        text = getEnergyText(
                            solarPanelService,
                            gps,
                            orientation,
                            compass,
                            alignToRestOfDay,
                            nowDuration,
                            formatter,
                            context.getString(R.string.up_to_amount)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("energy"),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
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
            position = TrailSenseGroupPosition(isFirst = false, isLast = true)
        )
    }
}

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
    position: TrailSenseGroupPosition
) {
    // Aligned is a tone change, not a bounce.
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val containerColor by animateColorAsState(
        targetValue = if (isComplete) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = effects,
        label = "solarReadoutContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isComplete) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = effects,
        label = "solarReadoutContent"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = if (position.isFirst) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
            topEnd = if (position.isFirst) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
            bottomStart = if (position.isLast) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
            bottomEnd = if (position.isLast) TrailSenseShape.groupOuter else TrailSenseShape.groupInner
        ),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.testTag(labelTag),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isComplete) contentColor else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = desired,
                    modifier = Modifier.testTag(desiredTag),
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum")
                )
                Text(
                    text = current,
                    modifier = Modifier.testTag(currentTag),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = contentColor.copy(alpha = 0.72f)
                )
            }
            SolarCue(
                isComplete = isComplete,
                completeTag = completeTag,
                arrowRotation = arrowRotation,
                arrowTag = arrowTag
            )
        }
    }
}

/** A check when aligned; otherwise the way to move, or nothing while within a step of it. */
@Composable
private fun SolarCue(
    isComplete: Boolean,
    completeTag: String,
    arrowRotation: Float?,
    arrowTag: String
) {
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        when {
            isComplete -> Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .testTag(completeTag),
                tint = colorResource(R.color.positive)
            )

            arrowRotation != null -> Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(rotationZ = arrowRotation)
                            .testTag(arrowTag)
                    )
                }
            }
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
    upToFormat: String
): String {
    val energy = solarPanelService.getSolarEnergy(
        gps.location,
        orientation.y,
        compass.bearing.inverse(),
        if (alignToRestOfDay) Duration.ofDays(1) else nowDuration,
        restrictToToday = alignToRestOfDay
    )
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
