@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import kotlinx.coroutines.launch

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.IStrideLengthEstimator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.StrideLengthEstimatorFactory
import kotlinx.coroutines.delay

@Composable
fun StrideLengthEstimationToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val estimator = remember(context) {
        StrideLengthEstimatorFactory(context).getEstimator()
    }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val prefs = remember(context) { UserPreferences(context) }
    val units = remember(prefs) { prefs.baseDistanceUnits }

    StrideLengthScreen(
        estimator = estimator,
        strideLengthText = {
            formatter.formatDistance(it.convertTo(units), 2, false)
        },
        onSave = {
            prefs.pedometer.strideLength = estimator.strideLength ?: Distance.meters(0f)
            effects.showMessage(context.getString(R.string.saved))
        },
        onPermissionDenied = {
            effects.explainPermissionDenied(AppPermission.ActivityRecognition)
        },
        requestPermission = { callback ->
            scope.launch { callback(effects.requestPermission(AppPermission.ActivityRecognition)) }
        }
    )
}

/**
 * Estimate stride length: the estimate as the hero, what to do right now inside it while measuring, and the one
 * action (start, stop, or save) as a full-width button below, with reset beside it only when there is something to
 * reset.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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

    val statusText = when {
        isRunning && !hasValidReading -> stringResource(R.string.stride_length_stand_still)
        isRunning -> stringResource(R.string.stride_length_walk)
        else -> null
    }

    TrailSensePage(
        title = stringResource(R.string.estimate_stride_length),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        item(key = "hero") {
            StrideLengthHero(
                value = strideLength?.let(strideLengthText) ?: stringResource(R.string.dash),
                status = statusText,
                isRunning = isRunning
            )
        }

        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
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

                // Reset only when stopped with a reading to clear.
                AnimatedVisibility(
                    visible = !isRunning && strideLength != null,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                ) {
                    TrailSenseButton(
                        text = stringResource(R.string.reset),
                        onClick = {
                            estimator.reset()
                            refreshState()
                        },
                        style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StrideLengthHero(value: String, status: String?, isRunning: Boolean) {
    val container by animateColorAsState(
        if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strideHeroContainer"
    )
    val content by animateColorAsState(
        if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strideHeroContent"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            Text(
                text = stringResource(R.string.pref_stride_length_title),
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                maxLines = 1
            )
            AnimatedVisibility(
                visible = status != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                Text(
                    text = status.orEmpty(),
                    modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                    style = MaterialTheme.typography.bodyLarge,
                    color = content.copy(alpha = 0.85f)
                )
            }
        }
    }
}
