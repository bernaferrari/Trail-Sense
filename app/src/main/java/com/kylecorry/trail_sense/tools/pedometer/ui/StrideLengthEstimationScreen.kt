@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.IStrideLengthEstimator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.StrideLengthEstimatorFactory
import kotlinx.coroutines.delay

@Composable
fun StrideLengthEstimationToolScreen(host: TrailSenseDestinationHost) {
    val estimator = remember(host) {
        StrideLengthEstimatorFactory(host.requireContext()).getEstimator()
    }
    val formatter = remember(host) { FormatService.getInstance(host.requireContext()) }
    val prefs = remember(host) { UserPreferences(host.requireContext()) }
    val units = remember(prefs) { prefs.baseDistanceUnits }

    StrideLengthScreen(
        estimator = estimator,
        strideLengthText = {
            formatter.formatDistance(it.convertTo(units), 2, false)
        },
        onSave = {
            prefs.pedometer.strideLength = estimator.strideLength ?: Distance.meters(0f)
            Alerts.toast(host.requireContext(), host.getString(R.string.saved))
        },
        onPermissionDenied = {
            host.alertNoActivityRecognitionPermission()
        },
        requestPermission = { callback ->
            host.requestActivityRecognition(callback)
        }
    )
}

@Composable
private fun StrideLengthScreen(
    estimator: IStrideLengthEstimator,
    strideLengthText: (Distance) -> String,
    onSave: () -> Unit,
    onPermissionDenied: () -> Unit,
    requestPermission: (((Boolean) -> Unit)) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var strideLength by remember { mutableStateOf(estimator.strideLength) }
    var hasValidReading by remember { mutableStateOf(estimator.hasValidReading) }

    fun onStrideLengthChanged(): Boolean {
        return true
    }

    fun refreshState() {
        strideLength = estimator.strideLength
        hasValidReading = estimator.hasValidReading
    }

    fun stop() {
        isRunning = false
        estimator.stop(::onStrideLengthChanged)
        refreshState()
    }

    fun start() {
        requestPermission { hasPermission ->
            if (hasPermission) {
                isRunning = true
                estimator.start(::onStrideLengthChanged)
            } else {
                isRunning = false
                onPermissionDenied()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshState()
            delay(33)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isRunning) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> estimator.stop(::onStrideLengthChanged)
                Lifecycle.Event.ON_RESUME -> if (isRunning) estimator.start(::onStrideLengthChanged)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            estimator.stop(::onStrideLengthChanged)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            // Hero readout: estimated stride length
            StrideLengthHero(
                value = strideLength?.let(strideLengthText) ?: stringResource(R.string.dash)
            )

            // Status / instructions card
            val statusText = when {
                isRunning && !hasValidReading -> stringResource(R.string.stride_length_stand_still)
                isRunning -> stringResource(R.string.stride_length_walk)
                else -> null
            }
            if (statusText != null) {
                TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.estimate_stride_length),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Primary start / stop / save action
            TrailSenseButton(
                text = when {
                    !isRunning && hasValidReading -> stringResource(R.string.save)
                    !isRunning -> stringResource(R.string.start)
                    else -> stringResource(R.string.stop)
                },
                onClick = {
                    when {
                        !isRunning && hasValidReading -> onSave()
                        !isRunning -> start()
                        else -> stop()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Secondary reset action (only when stopped with a reading)
            if (!isRunning && strideLength != null) {
                TrailSenseButton(
                    text = stringResource(R.string.reset),
                    onClick = {
                        estimator.reset()
                        refreshState()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StrideLengthHero(value: String) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "strideLengthHeroScale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TrailSenseSpacing.xxl, horizontal = TrailSenseSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.pref_stride_length_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.scale(scale),
                    text = value,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
