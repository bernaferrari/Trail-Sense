@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
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

@Composable
private fun MetalDetectorScreen(
    title: String,
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetalDetectorHeader(
                title = title,
                audioEnabled = audioEnabled,
                vibrationEnabled = vibrationEnabled,
                showAudioButton = showAudioButton,
                metalDetected = metalDetected,
                onAudioToggle = onAudioToggle,
                onVibrationToggle = onVibrationToggle
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.threshold),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = thresholdText,
                            modifier = Modifier.testTag("threshold_amount"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ThresholdSlider(
                        value = thresholdSliderValue,
                        onValueChange = onThresholdChange
                    )
                }
                Button(onClick = onCalibrate) {
                    Text(stringResource(R.string.calibrate))
                }
            }
            HighSensitivitySwitch(
                checked = isHighSensitivity,
                onCheckedChange = onHighSensitivityChange
            )
            AccuracyBadge(
                magnetometer = magnetometer,
                onClick = onAccuracyClick
            )
            if (showMetalDirection) {
                MagnetometerDirection(
                    fieldStrength = metalDirectionFieldStrength,
                    direction = metalDirection,
                    singlePole = showSinglePoleDirection,
                    sensitivity = metalDirectionSensitivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            MetalChart(
                data = chartReadings,
                lowerThreshold = lowerThreshold,
                upperThreshold = upperThreshold
            )
            Text(
                text = stringResource(R.string.metal_detector_disclaimer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetalDetectorHeader(
    title: String,
    audioEnabled: Boolean,
    vibrationEnabled: Boolean,
    showAudioButton: Boolean,
    metalDetected: Boolean,
    onAudioToggle: () -> Unit,
    onVibrationToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onVibrationToggle,
                modifier = Modifier
                    .alpha(if (vibrationEnabled) 1f else 0.38f)
                    .testTag("metal_vibration_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.vibrate),
                    contentDescription = null
                )
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metal_detector_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (metalDetected) {
                Icon(
                    painter = painterResource(R.drawable.metal),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (showAudioButton) {
                IconButton(
                    onClick = onAudioToggle,
                    modifier = Modifier
                        .alpha(if (audioEnabled) 1f else 0.38f)
                        .testTag("metal_audio_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.volume_up),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value.coerceIn(0f, 100f),
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("threshold"),
        valueRange = 0f..100f
    )
}

@Composable
private fun HighSensitivitySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.high_sensitivity),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("high_sensitivity_toggle")
        )
    }
}

@Composable
private fun AccuracyBadge(
    magnetometer: ISensor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasCompass = androidx.compose.runtime.remember {
        SensorService(context).hasCompass()
    }
    if (!hasCompass) {
        return
    }

    val provider = androidx.compose.runtime.remember(magnetometer, context) {
        SensorStatusBadgeProvider(magnetometer, context, R.drawable.ic_compass_icon)
    }
    var badge by androidx.compose.runtime.remember {
        mutableStateOf(provider.getBadge())
    }

    androidx.compose.runtime.LaunchedEffect(provider) {
        while (true) {
            badge = provider.getBadge()
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp)
            .alpha(0.6f)
            .clickable(onClick = onClick)
            .testTag("accuracy_view"),
        shape = RoundedCornerShape(4.dp),
        color = Color(badge.color),
        contentColor = Color.Black
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(badge.icon),
                contentDescription = null,
                modifier = Modifier.height(16.dp),
                tint = Color.Black
            )
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black
            )
        }
    }
}

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
    val lineColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.testTag("magnetometerView"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = min(size.height / 2f * 0.75f, size.width / 2f * 0.75f)
            val indicatorSize = 20.dp.toPx()

            drawCircle(
                color = lineColor,
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            if (fieldStrength >= sensitivity) {
                rotate(-direction.first.value, pivot = center) {
                    if (singlePole) {
                        rotate(
                            if (direction.first.value in 90f..270f) 180f else 0f,
                            pivot = center
                        ) {
                            drawCircle(
                                color = Color(AppColor.Green.color),
                                radius = indicatorSize,
                                center = androidx.compose.ui.geometry.Offset(
                                    center.x,
                                    center.y - radius
                                )
                            )
                        }
                    } else {
                        drawCircle(
                            color = Color(AppColor.Red.color),
                            radius = indicatorSize,
                            center = androidx.compose.ui.geometry.Offset(center.x, center.y - radius)
                        )
                        rotate(180f, pivot = center) {
                            drawCircle(
                                color = Color(AppColor.Blue.color),
                                radius = indicatorSize,
                                center = androidx.compose.ui.geometry.Offset(
                                    center.x,
                                    center.y - radius
                                )
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = formatter.formatMagneticField(fieldStrength),
            style = MaterialTheme.typography.titleMedium,
            color = lineColor
        )
    }
}

@Composable
private fun MetalChart(
    data: List<Float>,
    lowerThreshold: Float,
    upperThreshold: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(horizontal = 16.dp)
            .testTag("metal_chart"),
        contentAlignment = Alignment.Center
    ) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
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

            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawRect(
                color = Color(AppColor.Gray.color).copy(alpha = 0.2f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, y(upperThreshold)),
                size = androidx.compose.ui.geometry.Size(
                    size.width,
                    (y(lowerThreshold) - y(upperThreshold)).coerceAtLeast(0f)
                )
            )

            data.zipWithNext().forEachIndexed { index, (start, end) ->
                drawLine(
                    color = primary,
                    start = androidx.compose.ui.geometry.Offset(x(index), y(start)),
                    end = androidx.compose.ui.geometry.Offset(x(index + 1), y(end)),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
