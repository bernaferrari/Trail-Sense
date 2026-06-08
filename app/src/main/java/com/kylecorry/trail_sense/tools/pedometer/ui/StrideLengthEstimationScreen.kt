@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.IStrideLengthEstimator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.StrideLengthEstimatorFactory
import kotlinx.coroutines.delay

@Composable
fun StrideLengthEstimationToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val estimator = remember(fragment) {
        StrideLengthEstimatorFactory(fragment.requireContext()).getEstimator()
    }
    val formatter = remember(fragment) { FormatService.getInstance(fragment.requireContext()) }
    val prefs = remember(fragment) { UserPreferences(fragment.requireContext()) }
    val units = remember(prefs) { prefs.baseDistanceUnits }

    StrideLengthScreen(
        estimator = estimator,
        strideLengthText = {
            formatter.formatDistance(it.convertTo(units), 2, false)
        },
        onSave = {
            prefs.pedometer.strideLength = estimator.strideLength ?: Distance.meters(0f)
            Alerts.toast(fragment.requireContext(), fragment.getString(R.string.saved))
        },
        onPermissionDenied = {
            fragment.alertNoActivityRecognitionPermission()
        },
        requestPermission = { callback ->
            fragment.requestActivityRecognition(callback)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = strideLength?.let(strideLengthText) ?: stringResource(R.string.dash),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isRunning && !hasValidReading -> stringResource(R.string.stride_length_stand_still)
                    isRunning -> stringResource(R.string.stride_length_walk)
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            !isRunning && hasValidReading -> onSave()
                            !isRunning -> start()
                            else -> stop()
                        }
                    }
                ) {
                    Text(
                        text = when {
                            !isRunning && hasValidReading -> stringResource(R.string.save)
                            !isRunning -> stringResource(R.string.start)
                            else -> stringResource(R.string.stop)
                        }
                    )
                }
                if (!isRunning && strideLength != null) {
                    IconButton(
                        onClick = {
                            estimator.reset()
                            refreshState()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                }
            }
        }
    }
}
