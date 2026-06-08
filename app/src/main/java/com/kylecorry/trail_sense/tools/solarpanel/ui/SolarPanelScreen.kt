@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.solarpanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.andromeda.sense.compass.ICompass
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
import com.kylecorry.trail_sense.tools.solarpanel.domain.SolarPanelService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import kotlin.math.absoluteValue

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SolarArrow(
                visible = position != null && getAltitudeDiff(position, orientation) > ALTITUDE_THRESHOLD,
                rotation = 0f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .testTag("arrow_up")
            )
            SolarArrow(
                visible = position != null && getAltitudeDiff(position, orientation) < -ALTITUDE_THRESHOLD,
                rotation = 180f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .testTag("arrow_down")
            )
            SolarArrow(
                visible = position != null && getAzimuthDiff(position, compass, declination, prefs) < -AZIMUTH_THRESHOLD,
                rotation = 270f,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .testTag("arrow_left")
            )
            SolarArrow(
                visible = position != null && getAzimuthDiff(position, compass, declination, prefs) > AZIMUTH_THRESHOLD,
                rotation = 90f,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .testTag("arrow_right")
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SolarModeButton(
                        text = formatter.formatDuration(nowDuration),
                        selected = !alignToRestOfDay,
                        onClick = {
                            showDurationPicker = true
                        },
                        modifier = Modifier.testTag("solar_now_btn")
                    )
                    SolarModeButton(
                        text = stringResource(R.string.today),
                        selected = alignToRestOfDay,
                        onClick = {
                            alignToRestOfDay = true
                        },
                        modifier = Modifier.testTag("solar_today_btn")
                    )
                }
                position?.let {
                    Text(
                        text = getEnergyText(solarPanelService, gps, orientation, compass, alignToRestOfDay, nowDuration, formatter, context.getString(R.string.up_to_amount)),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .testTag("energy"),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (position == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("solar_loading")
                )
            } else {
                SolarPanelReadout(
                    position = position!!,
                    compass = compass,
                    orientation = orientation,
                    formatter = formatter,
                    prefs = prefs,
                    declination = declination,
                    frame = frame,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 72.dp)
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

@Composable
private fun SolarModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    }
}

@Composable
private fun SolarArrow(
    visible: Boolean,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    if (!visible) {
        Spacer(modifier = modifier.size(32.dp))
        return
    }

    Icon(
        painter = painterResource(R.drawable.ic_arrow),
        contentDescription = null,
        modifier = modifier
            .size(32.dp)
            .graphicsLayer(rotationZ = rotation),
        tint = MaterialTheme.colorScheme.primary
    )
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
    val azimuthAligned = getAzimuthDiff(position, compass, declination, prefs).absoluteValue < AZIMUTH_THRESHOLD
    val altitudeAligned = getAltitudeDiff(position, orientation).absoluteValue < ALTITUDE_THRESHOLD

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(56.dp)
    ) {
        SolarReadoutRow(
            label = stringResource(R.string.tilt),
            labelTag = "tilt_label",
            current = formatter.formatDegrees(orientation.y),
            currentTag = "current_altitude",
            desired = formatter.formatDegrees(position.first),
            desiredTag = "desired_altitude",
            isComplete = altitudeAligned,
            completeTag = "altitude_complete"
        )
        SolarReadoutRow(
            label = stringResource(R.string.compass_azimuth),
            labelTag = "azimuth_label",
            current = formatter.formatDegrees(compass.rawBearing, replace360 = true),
            currentTag = "current_azimuth",
            desired = formatter.formatDegrees(desiredAzimuth.value, replace360 = true),
            desiredTag = "desired_azimuth",
            isComplete = azimuthAligned,
            completeTag = "azimuth_complete"
        )
    }
}

@Composable
private fun SolarReadoutRow(
    label: String,
    labelTag: String,
    current: String,
    currentTag: String,
    desired: String,
    desiredTag: String,
    isComplete: Boolean,
    completeTag: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier.testTag(labelTag),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(24.dp))
            if (isComplete) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(completeTag),
                    tint = colorResource(R.color.positive)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(completeTag)
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = current,
                modifier = Modifier.testTag(currentTag),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = desired,
                modifier = Modifier.testTag(desiredTag),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
