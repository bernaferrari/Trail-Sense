package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.LabelFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.andromeda.sound.ToneGenerator
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.Slider
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.absoluteValue

class FragmentToolMetalDetector : Fragment() {
    private val sensors by lazy { SensorService(requireContext()) }
    private val magnetometer by lazy { sensors.getMagnetometer() }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val lowPassMagnetometer by lazy { sensors.getMagnetometer(true) }
    private val orientation by lazy { SensorService(requireContext()).getGyroscope() }
    private val gravity by lazy { sensors.getGravity() }

    private val filter = LowPassFilter(0.2f, 0f)

    private var isVibrating = false
    private var chart: MetalDetectorChart? = null
    private var magnetometerView: MagnetometerView? = null
    private var thresholdSlider: Slider? = null
    private var lastReadingTime = System.currentTimeMillis() + 1000L
    private var threshold = 5f

    private val readings = mutableListOf<Float>()

    private val throttle = Throttle(20)
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var isHighSensitivity by mutableStateOf(false)
    private var calibratedField = Vector3.zero
    private var calibratedOrientation = Quaternion.zero
    private var referenceMagnitude = 0f

    private val calibrateTimer = CoroutineTimer {
        calibrate()
    }

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }

    private val isMetalDetected = Debouncer(Duration.ofMillis(100))

    private var audio: ISoundPlayer? = null
    private val audioLock = Any()

    private var title by mutableStateOf("")
    private var thresholdText by mutableStateOf("")
    private var audioEnabled by mutableStateOf(false)
    private var vibrationEnabled by mutableStateOf(true)
    private var showAudioButton by mutableStateOf(true)
    private var metalDetected by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            keepScreenOn = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
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
                        onThresholdCreated = { thresholdSlider = it },
                        onChartCreated = {
                            if (chart == null) {
                                chart = MetalDetectorChart(
                                    it,
                                    Resources.getPrimaryColor(requireContext())
                                )
                            }
                        },
                        onMagnetometerCreated = { magnetometerView = it },
                        onAccuracyCreated = { accuracyView ->
                            accuracyView.setSensors(null, magnetometer)
                            accuracyView.setOnClickListener { displayAccuracyTips() }
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioEnabled = prefs.metalDetector.isMetalAudioEnabled
        vibrationEnabled = !prefs.metalDetector.isMetalVibrationDisabled
        thresholdText = formatService.formatMagneticField(threshold, decimalPlaces = 1)
    }

    override fun onResume() {
        super.onResume()
        magnetometerView?.setSinglePoleMode(prefs.metalDetector.showSinglePole)
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

    override fun onPause() {
        super.onPause()
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

    override fun onDestroyView() {
        chart = null
        magnetometerView = null
        thresholdSlider = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
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
            magnetometerView?.setFieldStrength(metal.magnitude())
            magnetometerView?.setMetalDirection(direction)
            magnetometerView?.setSensitivity(prefs.metalDetector.directionSensitivity)
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
        threshold = ((thresholdSlider?.value ?: 50f) / 10f).coerceAtLeast(0.1f)
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
        chart?.plot(readings, referenceMagnitude - threshold, referenceMagnitude + threshold)
    }

    private fun onMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    private fun initializeAudio() {
        viewLifecycleOwner.lifecycleScope.launch {
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
        context ?: return
        val alerter = ImproveAccuracyAlerter(requireContext())
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
    onThresholdCreated: (Slider) -> Unit,
    onChartCreated: (Chart) -> Unit,
    onMagnetometerCreated: (MagnetometerView) -> Unit,
    onAccuracyCreated: (SensorStatusBadgeView) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
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
                    ThresholdSlider(onThresholdCreated)
                }
                Button(onClick = onCalibrate) {
                    Text(stringResource(R.string.calibrate))
                }
            }
            HighSensitivitySwitch(
                checked = isHighSensitivity,
                onCheckedChange = onHighSensitivityChange
            )
            AccuracyBadge(onAccuracyCreated)
            if (showMetalDirection) {
                MagnetometerDirection(
                    onMagnetometerCreated = onMagnetometerCreated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            MetalChart(onChartCreated)
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
    onThresholdCreated: (Slider) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("threshold"),
        factory = { context ->
            Slider(context, null).apply {
                id = R.id.threshold
                contentDescription = context.getString(R.string.threshold)
                valueFrom = 0f
                valueTo = 100f
                value = 50f
                labelBehavior = LabelFormatter.LABEL_GONE
                onThresholdCreated(this)
            }
        }
    )
}

@Composable
private fun HighSensitivitySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("high_sensitivity_toggle"),
        factory = { context ->
            MaterialSwitch(context).apply {
                id = R.id.high_sensitivity_toggle
                text = context.getString(R.string.high_sensitivity)
                setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        },
        update = {
            if (it.isChecked != checked) {
                it.isChecked = checked
            }
        }
    )
}

@Composable
private fun AccuracyBadge(
    onAccuracyCreated: (SensorStatusBadgeView) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp)
            .testTag("accuracy_view"),
        factory = { context ->
            SensorStatusBadgeView(context).apply {
                id = R.id.accuracy_view
                alpha = 0.6f
                onAccuracyCreated(this)
            }
        }
    )
}

@Composable
private fun MagnetometerDirection(
    onMagnetometerCreated: (MagnetometerView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("magnetometerView"),
        factory = { context ->
            MagnetometerView(context, null).apply {
                id = R.id.magnetometerView
                onMagnetometerCreated(this)
            }
        }
    )
}

@Composable
private fun MetalChart(
    onChartCreated: (Chart) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(horizontal = 16.dp)
            .testTag("metal_chart"),
        factory = { context ->
            Chart(context).apply {
                id = R.id.metal_chart
                onChartCreated(this)
            }
        }
    )
}
