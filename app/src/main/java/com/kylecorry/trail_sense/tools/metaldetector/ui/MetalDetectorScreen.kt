@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.metaldetector.ui

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.wrapContentHeight

import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.toPath
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Brush
import com.kylecorry.trail_sense.shared.icons.trail.Recenter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.Vibrate
import com.kylecorry.trail_sense.shared.icons.trail.VolumeUp
import com.kylecorry.trail_sense.shared.icons.trail.CompassIcon
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Metal
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.icons.trail.KeyboardArrowRight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.andromeda.sound.ToneGenerator
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@Composable
fun MetalDetectorToolScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel = viewModel {
        MetalDetectorViewModel(
            sensors = SensorService(appContext),
            prefs = UserPreferences(appContext),
            haptics = HapticSubsystem.getInstance(appContext)
        )
    }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    MetalDetectorScreen(
        title = viewModel.magneticField?.let { formatService.formatMagneticField(it) } ?: "",
        fieldValue = viewModel.magneticField?.let { DecimalFormatter.format(it.toDouble(), 0) } ?: "--",
        fieldUnit = stringResource(R.string.magnetic_field_format_precise, "").trim(),
        thresholdText = formatService.formatMagneticField(viewModel.threshold, decimalPlaces = 1),
        isHighSensitivity = viewModel.isHighSensitivity,
        audioEnabled = viewModel.audioEnabled,
        vibrationEnabled = viewModel.vibrationEnabled,
        showAudioButton = viewModel.showAudioButton,
        showMetalDirection = viewModel.showMetalDirection,
        metalDetected = viewModel.metalDetected,
        onAudioToggle = viewModel::toggleAudio,
        onVibrationToggle = viewModel::toggleVibration,
        onHighSensitivityChange = viewModel::updateHighSensitivity,
        onCalibrate = viewModel::calibrate,
        thresholdSliderValue = viewModel.thresholdSliderValue,
        onThresholdChange = viewModel::changeThresholdSlider,
        chartReadings = viewModel.chartReadings,
        lowerThreshold = viewModel.lowerThreshold,
        upperThreshold = viewModel.upperThreshold,
        metalDirectionFieldStrength = viewModel.metalDirectionFieldStrength,
        metalDirection = viewModel.metalDirection,
        showSinglePoleDirection = viewModel.showSinglePoleDirection,
        metalDirectionSensitivity = viewModel.metalDirectionSensitivity,
        magnetometer = viewModel.magnetometer,
        onAccuracyClick = { ImproveAccuracyAlerter(context).alert(listOf(viewModel.magnetometer)) },
        // The screen stays on while detecting.
        modifier = modifier.keepScreenOn()
    )
}

/**
 * The metal detector's state: the magnetometer (and, for the direction dial, the low-pass magnetometer, gyroscope
 * and gravity) while the page is resumed, the calibrated reference, the threshold, the trace, and the sound and
 * vibration that say metal is near.
 */
internal class MetalDetectorViewModel(
    sensors: SensorService,
    private val prefs: UserPreferences,
    private val haptics: HapticSubsystem
) : ViewModel() {
    val magnetometer = sensors.getMagnetometer()
    private val lowPassMagnetometer = sensors.getMagnetometer(true)
    private val orientation = sensors.getGyroscope()
    private val gravity = sensors.getGravity()

    private val filter = LowPassFilter(0.2f, 0f)

    private var isVibrating = false
    private var lastReadingTime = System.currentTimeMillis() + 1000L

    private val readings = mutableListOf<Float>()

    private val throttle = Throttle(20)

    private var calibratedField = Vector3.zero
    private var calibratedOrientation = Quaternion.zero
    private var referenceMagnitude = 0f

    private val calibrateTimer = CoroutineTimer {
        calibrate()
    }

    private val isMetalDetected = Debouncer(Duration.ofMillis(100))

    private var audio: ISoundPlayer? = null
    private val audioLock = Any()

    /** The field strength shown, or null before the first reading. */
    var magneticField by mutableStateOf<Float?>(null)
        private set
    var threshold by mutableFloatStateOf(5f)
        private set
    var isHighSensitivity by mutableStateOf(false)
        private set
    var audioEnabled by mutableStateOf(prefs.metalDetector.isMetalAudioEnabled)
        private set
    var vibrationEnabled by mutableStateOf(!prefs.metalDetector.isMetalVibrationDisabled)
        private set
    var showAudioButton by mutableStateOf(true)
        private set
    var metalDetected by mutableStateOf(false)
        private set
    var thresholdSliderValue by mutableFloatStateOf(50f)
        private set
    var metalDirectionFieldStrength by mutableFloatStateOf(0f)
        private set
    var metalDirection by mutableStateOf(Bearing.from(0f) to Bearing.from(180f))
        private set
    var showSinglePoleDirection by mutableStateOf(false)
        private set
    var metalDirectionSensitivity by mutableFloatStateOf(1f)
        private set
    var chartReadings by mutableStateOf<List<Float>>(emptyList())
        private set
    var lowerThreshold by mutableFloatStateOf(0f)
        private set
    var upperThreshold by mutableFloatStateOf(0f)
        private set

    val showMetalDirection: Boolean
        get() = prefs.metalDetector.showMetalDirection

    fun changeThresholdSlider(value: Float) {
        thresholdSliderValue = value
    }

    fun onResume() {
        showSinglePoleDirection = prefs.metalDetector.showSinglePole
        magnetometer.start(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.start(this::onLowPassMagnetometerUpdate)
            orientation.start(this::onMagnetometerUpdate)
            gravity.start(this::onMagnetometerUpdate)
        }
        calibrateTimer.once(Duration.ofSeconds(2))

        if (prefs.metalDetector.isMetalAudioEnabled) {
            initializeAudio()
        }
    }

    fun onPause() {
        magnetometer.stop(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.stop(this::onLowPassMagnetometerUpdate)
            orientation.stop(this::onMagnetometerUpdate)
            gravity.stop(this::onMagnetometerUpdate)
        }
        calibrateTimer.stop()
        haptics.off()
        isVibrating = false
        audio?.off()
    }

    override fun onCleared() {
        onPause()
        audio?.release()
    }

    fun updateHighSensitivity(isChecked: Boolean) {
        isHighSensitivity = isChecked
        isMetalDetected.debounceTime = if (isChecked) {
            Duration.ofMillis(50)
        } else {
            Duration.ofMillis(100)
        }
    }

    fun toggleAudio() {
        if (prefs.metalDetector.isMetalAudioEnabled) {
            prefs.metalDetector.isMetalAudioEnabled = false
            audioEnabled = false
            audio?.off()
        } else {
            prefs.metalDetector.isMetalAudioEnabled = true
            audioEnabled = true
            initializeAudio()
        }
    }

    fun toggleVibration() {
        if (prefs.metalDetector.isMetalVibrationDisabled) {
            prefs.metalDetector.isMetalVibrationDisabled = false
        } else {
            prefs.metalDetector.isMetalVibrationDisabled = true
            haptics.off()
        }
        vibrationEnabled = !prefs.metalDetector.isMetalVibrationDisabled
    }

    fun calibrate() {
        referenceMagnitude = readings.takeLast(20).average().toFloat()
        calibratedField = lowPassMagnetometer.magneticField
        calibratedOrientation = orientation.orientation
        calibrateTimer.stop()
    }

    private fun onLowPassMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {
        if (throttle.isThrottled()) {
            return
        }

        if (prefs.metalDetector.showMetalDirection) {
            val metal = Physics.removeGeomagneticField(
                lowPassMagnetometer.magneticField,
                calibratedField,
                null
            )
            val direction = Physics.getMetalDirection(
                metal,
                gravity.acceleration
            )
            metalDirectionFieldStrength = metal.magnitude()
            metalDirection = direction
            metalDirectionSensitivity = prefs.metalDetector.directionSensitivity
        }

        val magneticField = getCurrentMagneticFieldStrength()

        if (canAddReading(magneticField)) {
            addReading(magneticField)
            updateChart()
        }

        updateThreshold()

        this.magneticField = magneticField

        metalDetected = isMetalDetected(magneticField)
        maybeVibrate(metalDetected)
        updateMetalSoundIntensity(magneticField)
    }

    private fun maybeVibrate(metalDetected: Boolean) {
        if (!metalDetected || prefs.metalDetector.isMetalVibrationDisabled) {
            isVibrating = false
            haptics.off()
            return
        }

        if (!isVibrating) {
            isVibrating = true
            haptics.interval(VIBRATION_DURATION)
        }
    }

    private fun updateMetalSoundIntensity(reading: Float) {
        if (!isMetalDetected.value || !prefs.metalDetector.isMetalAudioEnabled) {
            audio?.off()
            return
        }
        val delta = (reading - referenceMagnitude).absoluteValue
        val volume = Interpolation.map(delta - threshold, 0f, 30f, 0f, 1f, true)
        audio?.setVolume(volume)
        if (audio?.isOn() != true) {
            audio?.on()
        }
    }

    private fun getCurrentMagneticFieldStrength(): Float {
        val filtered = filter.filter(magnetometer.magneticField.magnitude())
        return if (isHighSensitivity) {
            magnetometer.magneticField.magnitude()
        } else {
            filtered
        }
    }

    private fun isMetalDetected(reading: Float): Boolean {
        val delta = (reading - referenceMagnitude).absoluteValue
        val current = delta >= threshold && referenceMagnitude != 0f

        isMetalDetected.update(current)
        return isMetalDetected.value
    }

    private fun canAddReading(reading: Float): Boolean {
        return System.currentTimeMillis() - lastReadingTime > 20 && reading != 0f
    }

    private fun updateThreshold() {
        threshold = (thresholdSliderValue / 10f).coerceAtLeast(0.1f)
    }

    private fun addReading(reading: Float) {
        readings.add(reading)
        if (readings.size > 150) {
            readings.removeAt(0)
        }
        lastReadingTime = System.currentTimeMillis()
    }

    private fun updateChart() {
        chartReadings = readings.toList()
        lowerThreshold = referenceMagnitude - threshold
        upperThreshold = referenceMagnitude + threshold
    }

    private fun onMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    private fun initializeAudio() {
        viewModelScope.launch {
            val initialized = onDefault {
                try {
                    synchronized(audioLock) {
                        if (audio == null) {
                            audio = SoundPlayer(ToneGenerator().getTone(AUDIO_FREQUENCY))
                        }
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            if (!initialized) {
                prefs.metalDetector.isMetalAudioEnabled = false
                audioEnabled = false
                showAudioButton = false
            }
        }
    }

    companion object {
        private val VIBRATION_DURATION = Duration.ofMillis(200)
        private const val AUDIO_FREQUENCY = 750
    }
}


/**
 * Metal detector: the field well is the hero, with the reading inside, the live trace round its rim, and the verdict
 * and one quiet accuracy line under it; the direction dial (when enabled) follows, then the threshold and
 * sensitivity controls as one group. Sound and vibration are toggles in the top bar.
 */
@Composable
private fun MetalDetectorScreen(
    title: String,
    fieldValue: String,
    fieldUnit: String,
    thresholdText: String,
    isHighSensitivity: Boolean,
    audioEnabled: Boolean,
    vibrationEnabled: Boolean,
    showAudioButton: Boolean,
    showMetalDirection: Boolean,
    metalDetected: Boolean,
    onAudioToggle: () -> Unit,
    onVibrationToggle: () -> Unit,
    onHighSensitivityChange: (Boolean) -> Unit,
    onCalibrate: () -> Unit,
    thresholdSliderValue: Float,
    onThresholdChange: (Float) -> Unit,
    chartReadings: List<Float>,
    lowerThreshold: Float,
    upperThreshold: Float,
    metalDirectionFieldStrength: Float,
    metalDirection: Pair<Bearing, Bearing>,
    showSinglePoleDirection: Boolean,
    metalDirectionSensitivity: Float,
    magnetometer: ISensor,
    onAccuracyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_metal_detector_title),
        modifier = modifier,
        actions = {
            ToolToggle(
                checked = vibrationEnabled,
                onToggle = onVibrationToggle,
                icon = TrailIcons.Vibrate,
                label = stringResource(R.string.tool_vibration),
                shake = metalDetected && vibrationEnabled,
                modifier = Modifier.testTag("metal_vibration_btn")
            )
            if (showAudioButton) {
                ToolToggle(
                    checked = audioEnabled,
                    onToggle = onAudioToggle,
                    icon = TrailIcons.VolumeUp,
                    label = stringResource(R.string.tool_sound),
                    modifier = Modifier.testTag("metal_audio_btn")
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                MetalHero(
                    title = title,
                    fieldValue = fieldValue,
                    fieldUnit = fieldUnit,
                    metalDetected = metalDetected,
                    magnetometer = magnetometer,
                    onAccuracyClick = onAccuracyClick,
                    chartReadings = chartReadings,
                    lowerThreshold = lowerThreshold,
                    upperThreshold = upperThreshold
                )

                if (showMetalDirection) {
                    MagnetometerDirection(
                        fieldStrength = metalDirectionFieldStrength,
                        direction = metalDirection,
                        singlePole = showSinglePoleDirection,
                        sensitivity = metalDirectionSensitivity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }

                // Threshold, calibration and sensitivity as one segmented group.
                Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    TrailSenseGroupCard(
                        position = TrailSenseGroupPosition(isFirst = true, isLast = false)
                    ) {
                        Text(
                            text = stringResource(R.string.threshold),
                            style = MaterialTheme.typography.titleMedium
                        )
                        ThresholdSlider(
                            value = thresholdSliderValue.coerceIn(0f, 100f),
                            label = thresholdText,
                            onValueChange = onThresholdChange
                        )
                    }
                    TrailSenseListItem(
                        title = stringResource(R.string.calibrate),
                        subtitle = stringResource(R.string.metal_detector_calibrate_hint),
                        icon = rememberVectorPainter(TrailIcons.Recenter),
                        position = TrailSenseGroupPosition(isFirst = false, isLast = false),
                        onClick = onCalibrate,
                        trailing = {
                            Icon(
                                TrailIcons.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    TrailSenseSwitchRow(
                        label = stringResource(R.string.high_sensitivity),
                        checked = isHighSensitivity,
                        onCheckedChange = onHighSensitivityChange,
                        position = TrailSenseGroupPosition(isFirst = false, isLast = true),
                        modifier = Modifier.testTag("high_sensitivity_toggle")
                    )
                }

                Text(
                    text = stringResource(R.string.metal_detector_disclaimer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TrailSenseSpacing.lg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The hero answers one question, "is there metal here, and how close?", with one living object: a field well whose
 * centre shape swells and morphs from a soft cookie into a spiky burst as the field strays from normal, with rings
 * rippling out of it faster the stronger it gets. The well's rim is the trace: the last few seconds of the field
 * run round it from the top, inside the dashed normal band while all is calm and out past it near metal. The
 * reading sits inside, the verdict under it, and the sensor's accuracy as one quiet line.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetalHero(
    title: String,
    fieldValue: String,
    fieldUnit: String,
    metalDetected: Boolean,
    magnetometer: ISensor,
    onAccuracyClick: () -> Unit,
    chartReadings: List<Float>,
    lowerThreshold: Float,
    upperThreshold: Float
) {
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val well by animateColorAsState(if (metalDetected) scheme.errorContainer else scheme.primaryContainer, effects, label = "well")
    val core by animateColorAsState(if (metalDetected) scheme.error else scheme.primary, effects, label = "core")
    val onCore by animateColorAsState(if (metalDetected) scheme.onError else scheme.onPrimary, effects, label = "onCore")
    val verdictColor by animateColorAsState(if (metalDetected) scheme.error else scheme.onSurface, effects, label = "verdict")
    val band = scheme.onPrimaryContainer
    val metalInk = scheme.error
    val now = scheme.tertiary

    // How far outside the normal band the latest reading is: 0 inside it, 1 well beyond. Nothing until calibrated.
    val last = chartReadings.lastOrNull()
    val halfBand = ((upperThreshold - lowerThreshold) / 2f).coerceAtLeast(0.1f)
    val middle = (upperThreshold + lowerThreshold) / 2f
    val calibrated = middle > 0.5f
    val target = if (last == null || !calibrated) {
        0f
    } else {
        ((kotlin.math.abs(last - middle) / halfBand - 0.5f) / 1.5f).coerceIn(0f, 1f)
    }
    val strength by animateFloatAsState(
        if (metalDetected) target.coerceAtLeast(0.6f) else target,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "strength"
    )

    val transition = rememberInfiniteTransition(label = "field")
    val ripple by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween((2400 - 1900 * target).toInt().coerceAtLeast(400), easing = LinearEasing)),
        label = "ripple"
    )
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "turn")
    val morph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Burst) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .widthIn(max = 360.dp)
                .testTag("metal_chart"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                // The rim (the trace's track) and the well, with three rings rippling out of the core inside it.
                drawCircle(well.copy(alpha = 0.55f), radius)
                drawCircle(well, radius * 0.78f)
                repeat(3) { i ->
                    val t = (ripple + i / 3f) % 1f
                    drawCircle(
                        core.copy(alpha = (0.10f + 0.35f * strength) * (1f - t)),
                        radius = radius * (0.42f + 0.36f * t),
                        style = Stroke(width = (2f + 3f * strength).dp.toPx())
                    )
                }

                // The rim: the normal band between two dashed circles, and the trace running round it.
                val mid = radius * 0.89f
                val reach = radius * 0.095f
                val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 5.dp.toPx())
                )
                listOf(mid - reach / 2f, mid + reach / 2f).forEach { r ->
                    drawCircle(band.copy(alpha = 0.35f), r, style = Stroke(width = 1.dp.toPx(), pathEffect = dash))
                }
                if (calibrated && chartReadings.size > 1) {
                    val count = chartReadings.size
                    fun point(age: Int, value: Float): androidx.compose.ui.geometry.Offset {
                        // Newest at the top; older readings run back anticlockwise.
                        val angle = Math.toRadians((-90.0 - age * 360.0 / TRACE_CAPACITY))
                        val deviation = ((value - middle) / halfBand).coerceIn(-2f, 2f) * reach / 2f
                        val r = mid + deviation
                        return androidx.compose.ui.geometry.Offset(
                            center.x + r * kotlin.math.cos(angle).toFloat(),
                            center.y + r * kotlin.math.sin(angle).toFloat()
                        )
                    }
                    for (age in 0 until count - 1) {
                        val newer = chartReadings[count - 1 - age]
                        val older = chartReadings[count - 2 - age]
                        val outside = kotlin.math.abs(newer - middle) > halfBand
                        drawLine(
                            color = if (outside) metalInk else core,
                            start = point(age, newer),
                            end = point(age + 1, older),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            alpha = 1f - age.toFloat() / TRACE_CAPACITY
                        )
                    }
                    // "Now" is the one accent: a plain tertiary dot where the trace is being written.
                    drawCircle(now, 4.dp.toPx(), point(0, chartReadings.last()))
                }

                // The core: grows and sharpens as the field strengthens.
                val coreSize = radius * 2f * (0.50f + 0.18f * strength)
                val path = morph.toPath(strength)
                val matrix = android.graphics.Matrix().apply {
                    setScale(coreSize, coreSize)
                    postTranslate(center.x - coreSize / 2f, center.y - coreSize / 2f)
                    postRotate(turn, center.x, center.y)
                }
                path.transform(matrix)
                drawPath(path.asComposePath(), core)
            }
            TrailSenseReading(
                value = fieldValue,
                unit = fieldUnit,
                color = onCore,
                unitColor = onCore.copy(alpha = 0.75f),
                style = MaterialTheme.typography.displayMedium,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .testTag("metal_detector_title")
                    .semantics { contentDescription = title }
            )
        }
        Text(
            text = stringResource(if (metalDetected) R.string.metal_detected else R.string.metal_not_detected),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = verdictColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TrailSenseSpacing.lg)
        )
        AccuracyBadge(magnetometer = magnetometer, onClick = onAccuracyClick)
    }
}

/** How many readings the rim holds: one full turn. */
private const val TRACE_CAPACITY = 150

/**
 * The threshold as one thick rounded track that fills to the value, with the value itself riding in the thumb, so
 * the control reads as one object rather than two bars and a dot.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdSlider(value: Float, label: String, onValueChange: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..100f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("threshold"),
        thumb = {
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm)
                        .testTag("threshold_amount"),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        },
        track = { state ->
            val fraction = ((state.value - state.valueRange.start) /
                (state.valueRange.endInclusive - state.valueRange.start)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
            }
        }
    )
}

/** A top-bar toggle: filled tonal while on, plain while off, always labelled. */
@Composable
private fun ToolToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    shake: Boolean = false
) {
    val wobble by rememberInfiniteTransition(label = "shake").animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeAngle"
    )
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.graphicsLayer { rotationZ = if (shake) wobble else 0f }
        )
    }
}

@Composable
private fun AccuracyBadge(
    magnetometer: ISensor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasCompass = remember { SensorService(context).hasCompass() }
    if (!hasCompass) {
        return
    }

    val provider = remember(magnetometer, context) {
        SensorStatusBadgeProvider(magnetometer, context, TrailIcons.CompassIcon)
    }
    var badge by remember { mutableStateOf(provider.getBadge()) }

    LaunchedEffect(provider) {
        while (true) {
            badge = provider.getBadge()
            delay(1000)
        }
    }

    // A plain label, no pill, dot or icon: the sensor's accuracy in words, tappable for the tips.
    Text(
        text = stringResource(R.string.metal_detector_accuracy, badge.name),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .heightIn(min = TrailSenseSize.minimumTouchTarget)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = TrailSenseSpacing.sm)
            .testTag("accuracy_view")
    )
}

/**
 * Where the metal is: a round tonal face with a thick primary ring; the poles show as solid dots on the ring (one in
 * primary for a single pole, error and primary for north and south) once the field is strong enough.
 */
@Composable
private fun MagnetometerDirection(
    fieldStrength: Float,
    direction: Pair<Bearing, Bearing>,
    singlePole: Boolean,
    sensitivity: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = androidx.compose.runtime.remember {
        FormatService.getInstance(context)
    }
    val reading = formatter.formatMagneticField(fieldStrength)
    val scheme = MaterialTheme.colorScheme
    val face = scheme.surfaceContainerLow
    val ring = scheme.primary.copy(alpha = 0.22f)
    val north = scheme.error
    val south = scheme.primary
    val detected = fieldStrength >= sensitivity

    Box(
        modifier = modifier
            .testTag("magnetometerView")
            .semantics { contentDescription = reading },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = min(size.height / 2f * 0.82f, size.width / 2f * 0.82f)
            val indicatorSize = 18.dp.toPx()
            drawCircle(face, radius + 22.dp.toPx(), center)
            drawCircle(ring, radius, center, style = Stroke(width = 12.dp.toPx()))

            if (detected) {
                rotate(-direction.first.value, pivot = center) {
                    if (singlePole) {
                        rotate(
                            if (direction.first.value in 90f..270f) 180f else 0f,
                            pivot = center
                        ) {
                            drawCircle(south, indicatorSize, androidx.compose.ui.geometry.Offset(center.x, center.y - radius))
                        }
                    } else {
                        drawCircle(north, indicatorSize, androidx.compose.ui.geometry.Offset(center.x, center.y - radius))
                        rotate(180f, pivot = center) {
                            drawCircle(south, indicatorSize, androidx.compose.ui.geometry.Offset(center.x, center.y - radius))
                        }
                    }
                }
            }
        }
        Text(
            text = reading,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = if (detected) scheme.primary else scheme.onSurface
        )
    }
}
