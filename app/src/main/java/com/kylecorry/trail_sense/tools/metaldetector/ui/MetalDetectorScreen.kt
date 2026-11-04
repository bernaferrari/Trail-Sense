@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.metaldetector.ui

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlinx.coroutines.CoroutineScope
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
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember {
        MetalDetectorController(context, scope)
    }

    DisposableEffect(lifecycleOwner, controller) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
            controller.onDestroy()
        }
    }

    controller.Content(modifier)
}

private class MetalDetectorController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val sensors by lazy { SensorService(context) }
    private val magnetometer by lazy { sensors.getMagnetometer() }
    private val formatService by lazy { FormatService.getInstance(context) }
    private val lowPassMagnetometer by lazy { sensors.getMagnetometer(true) }
    private val orientation by lazy { SensorService(context).getGyroscope() }
    private val gravity by lazy { sensors.getGravity() }

    private val filter = LowPassFilter(0.2f, 0f)

    private var isVibrating = false
    private var lastReadingTime = System.currentTimeMillis() + 1000L
    private var threshold = 5f

    private val readings = mutableListOf<Float>()

    private val throttle = Throttle(20)
    private val prefs by lazy { UserPreferences(context) }

    private var isHighSensitivity by mutableStateOf(false)
    private var calibratedField = Vector3.zero
    private var calibratedOrientation = Quaternion.zero
    private var referenceMagnitude = 0f

    private val calibrateTimer = CoroutineTimer {
        calibrate()
    }

    private val haptics by lazy { HapticSubsystem.getInstance(context) }

    private val isMetalDetected = Debouncer(Duration.ofMillis(100))

    private var audio: ISoundPlayer? = null
    private val audioLock = Any()

    private var title by mutableStateOf("")
    private var fieldValue by mutableStateOf("--")
    private val fieldUnit by lazy {
        context.getString(R.string.magnetic_field_format_precise, "").trim()
    }
    private var thresholdText by mutableStateOf("")
    private var audioEnabled by mutableStateOf(false)
    private var vibrationEnabled by mutableStateOf(true)
    private var showAudioButton by mutableStateOf(true)
    private var metalDetected by mutableStateOf(false)
    private var thresholdSliderValue by mutableStateOf(50f)
    private var metalDirectionFieldStrength by mutableStateOf(0f)
    private var metalDirection by mutableStateOf(Bearing.from(0f) to Bearing.from(180f))
    private var showSinglePoleDirection by mutableStateOf(false)
    private var metalDirectionSensitivity by mutableStateOf(1f)
    private var chartReadings by mutableStateOf<List<Float>>(emptyList())
    private var lowerThreshold by mutableStateOf(0f)
    private var upperThreshold by mutableStateOf(0f)

    init {
        audioEnabled = prefs.metalDetector.isMetalAudioEnabled
        vibrationEnabled = !prefs.metalDetector.isMetalVibrationDisabled
        thresholdText = formatService.formatMagneticField(threshold, decimalPlaces = 1)
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        MetalDetectorScreen(
            title = title,
            fieldValue = fieldValue,
            fieldUnit = fieldUnit,
            thresholdText = thresholdText,
            isHighSensitivity = isHighSensitivity,
            audioEnabled = audioEnabled,
            vibrationEnabled = vibrationEnabled,
            showAudioButton = showAudioButton,
            showMetalDirection = prefs.metalDetector.showMetalDirection,
            metalDetected = metalDetected,
            onAudioToggle = ::toggleAudio,
            onVibrationToggle = ::toggleVibration,
            onHighSensitivityChange = ::updateHighSensitivity,
            onCalibrate = ::calibrate,
            thresholdSliderValue = thresholdSliderValue,
            onThresholdChange = { thresholdSliderValue = it },
            chartReadings = chartReadings,
            lowerThreshold = lowerThreshold,
            upperThreshold = upperThreshold,
            metalDirectionFieldStrength = metalDirectionFieldStrength,
            metalDirection = metalDirection,
            showSinglePoleDirection = showSinglePoleDirection,
            metalDirectionSensitivity = metalDirectionSensitivity,
            magnetometer = magnetometer,
            onAccuracyClick = ::displayAccuracyTips,
            modifier = modifier
        )
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

    fun onDestroy() {
        audio?.release()
    }

    private fun updateHighSensitivity(isChecked: Boolean) {
        isHighSensitivity = isChecked
        isMetalDetected.debounceTime = if (isChecked) {
            Duration.ofMillis(50)
        } else {
            Duration.ofMillis(100)
        }
    }

    private fun toggleAudio() {
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

    private fun toggleVibration() {
        if (prefs.metalDetector.isMetalVibrationDisabled) {
            prefs.metalDetector.isMetalVibrationDisabled = false
        } else {
            prefs.metalDetector.isMetalVibrationDisabled = true
            haptics.off()
        }
        vibrationEnabled = !prefs.metalDetector.isMetalVibrationDisabled
    }

    private fun calibrate() {
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

        title = formatService.formatMagneticField(magneticField)
        fieldValue = DecimalFormatter.format(magneticField.toDouble(), 0)

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
        thresholdText = formatService.formatMagneticField(threshold, decimalPlaces = 1)
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
        scope.launch {
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

    private fun displayAccuracyTips() {
        val alerter = ImproveAccuracyAlerter(context)
        alerter.alert(listOf(magnetometer))
    }

    companion object {
        private val VIBRATION_DURATION = Duration.ofMillis(200)
        private const val AUDIO_FREQUENCY = 750
    }
}


/**
 * Metal detector: the field strength is the hero on a card that turns to the error tone while metal is near, with
 * the live chart under it; the direction dial (when enabled) follows, then the threshold and sensitivity controls
 * as one group. Sound and vibration are toggles in the top bar.
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

                Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    TrailSenseGroupCard(
                        position = TrailSenseGroupPosition(isFirst = true, isLast = false)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.threshold),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = thresholdText,
                                    modifier = Modifier
                                        .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs)
                                        .testTag("threshold_amount"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Slider(
                            value = thresholdSliderValue.coerceIn(0f, 100f),
                            onValueChange = onThresholdChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TrailSenseSpacing.xs)
                                .testTag("threshold"),
                            valueRange = 0f..100f
                        )
                        FilledTonalButton(
                            onClick = onCalibrate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(TrailIcons.Recenter, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(TrailSenseSpacing.sm))
                            Text(stringResource(R.string.calibrate))
                        }
                    }
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
 * The hero: the field strength large on a saturated card that flips from the calm primary tone to solid error when
 * metal is near. The badge turns into a burst and sends radar pings out while it detects; the live trace sits at the
 * bottom of the card as a filled area with the threshold band behind it.
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
    val container by animateColorAsState(
        if (metalDetected) scheme.error else scheme.primaryContainer,
        effects,
        label = "metalHero"
    )
    val content by animateColorAsState(
        if (metalDetected) scheme.onError else scheme.onPrimaryContainer,
        effects,
        label = "metalHeroContent"
    )
    val accent by animateColorAsState(
        if (metalDetected) scheme.onError else scheme.primary,
        effects,
        label = "metalAccent"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetalBadge(metalDetected = metalDetected, accent = accent, onAccent = container)
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.metal_detector_field),
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.78f)
                    )
                    Text(
                        text = stringResource(
                            if (metalDetected) R.string.metal_detected else R.string.metal_not_detected
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AccuracyBadge(magnetometer = magnetometer, onClick = onAccuracyClick)
            }
            TrailSenseReading(
                value = fieldValue,
                unit = fieldUnit,
                color = content,
                unitColor = content.copy(alpha = 0.72f),
                style = MaterialTheme.typography.displayLarge,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .testTag("metal_detector_title")
                    .semantics { contentDescription = title }
            )
            MetalChart(
                data = chartReadings,
                lowerThreshold = lowerThreshold,
                upperThreshold = upperThreshold,
                lineColor = accent,
                inkColor = content
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetalBadge(metalDetected: Boolean, accent: Color, onAccent: Color) {
    val ping by rememberInfiniteTransition(label = "ping").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "pingProgress"
    )
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        if (metalDetected) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(2) { i ->
                    val t = (ping + i * 0.5f) % 1f
                    drawCircle(
                        accent.copy(alpha = 0.5f * (1f - t)),
                        radius = size.minDimension / 2f * (0.7f + 0.6f * t),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
        TrailSenseShapeBadge(
            icon = TrailIcons.Metal,
            shape = if (metalDetected) MaterialShapes.Burst.toShape() else MaterialShapes.Cookie9Sided.toShape(),
            size = 56.dp,
            containerColor = accent,
            contentColor = onAccent
        )
    }
}

/** A top-bar toggle: filled tonal while on, plain while off, always labelled. */
@Composable
private fun ToolToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = label)
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

    // A quiet pill: the status color is a dot, the text stays in the card's tone.
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = TrailSenseSize.minimumTouchTarget)
            .testTag("accuracy_view"),
        shape = CircleShape,
        color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(0.dp)
            ) {
                Surface(shape = CircleShape, color = Color(badge.color), modifier = Modifier.fillMaxSize()) {}
            }
            Icon(
                imageVector = TrailIcons.CompassIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
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

/** The live trace: a thick line over a soft filled area, the threshold band behind it, dashed light gridlines. */
@Composable
private fun MetalChart(
    data: List<Float>,
    lowerThreshold: Float,
    upperThreshold: Float,
    lineColor: Color,
    inkColor: Color
) {
    val grid = inkColor.copy(alpha = 0.16f)
    val band = inkColor.copy(alpha = 0.10f)

    // Holds its height while the first readings arrive, so nothing jumps.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .testTag("metal_chart")
    ) {
        if (data.isEmpty()) {
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val minimum = min(30f, data.minOrNull() ?: 30f)
            val maximum = max(70f, data.maxOrNull() ?: 70f)
            val range = (maximum - minimum).takeIf { it > 0f } ?: 1f

            fun x(index: Int): Float {
                return if (data.size <= 1) {
                    size.width
                } else {
                    index.toFloat() / (data.size - 1).toFloat() * size.width
                }
            }

            fun y(value: Float): Float {
                return size.height - ((value - minimum) / range).coerceIn(0f, 1f) * size.height
            }

            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
            repeat(3) { index ->
                val y = (index + 1) / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash
                )
            }

            drawRoundRect(
                color = band,
                topLeft = androidx.compose.ui.geometry.Offset(0f, y(upperThreshold)),
                size = androidx.compose.ui.geometry.Size(
                    size.width,
                    (y(lowerThreshold) - y(upperThreshold)).coerceAtLeast(0f)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )

            val line = androidx.compose.ui.graphics.Path().apply {
                data.forEachIndexed { index, value ->
                    if (index == 0) moveTo(x(index), y(value)) else lineTo(x(index), y(value))
                }
            }
            val area = androidx.compose.ui.graphics.Path().apply {
                addPath(line)
                lineTo(x(data.lastIndex), size.height)
                lineTo(x(0), size.height)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f)))
            )
            drawPath(
                line,
                lineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            val last = data.last()
            drawCircle(lineColor, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(x(data.lastIndex), y(last)))
        }
    }
}
