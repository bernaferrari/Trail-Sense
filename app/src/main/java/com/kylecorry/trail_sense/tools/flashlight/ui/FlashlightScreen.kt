@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant

@Composable
fun FlashlightScreen(
    initialSelectedMode: FlashlightMode,
    onSelectedModeChanged: (FlashlightMode) -> Unit,
    onOpenScreenFlashlight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = remember { HapticSubsystem.getInstance(context) }
    val flashlight = remember { FlashlightSubsystem.getInstance(context) }
    val cache = remember { PreferencesSubsystem.getInstance(context).preferences }
    val prefs = remember { UserPreferences(context) }
    val formatter = remember { FormatService.getInstance(context) }
    val hasFlashlight = remember { flashlight.isAvailable() }
    val maxBrightness = remember { flashlight.brightnessLevels }
    val hasBrightnessControl = remember(maxBrightness) { maxBrightness > 0 }
    var flashlightMode by remember { mutableStateOf(FlashlightMode.Off) }
    var selectedMode by remember { mutableStateOf(initialSelectedMode) }
    var brightnessValue by remember { mutableStateOf(0f) }
    var timerText by remember { mutableStateOf<String?>(null) }
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    fun getTimerText(): String? {
        if (!prefs.flashlight.shouldTimeout) {
            return null
        }

        val instant = cache.getInstant(context.getString(R.string.pref_flashlight_timeout_instant))
        val duration = if (instant != null && instant.isAfter(Instant.now())) {
            Duration.between(Instant.now(), instant)
        } else {
            prefs.flashlight.timeout
        }

        return formatter.formatDuration(
            duration,
            short = false,
            includeSeconds = true
        )
    }

    fun refreshFlashlightState() {
        val mode = flashlight.getMode()
        flashlightMode = mode
        selectedMode = if (mode != FlashlightMode.Off) {
            flashlight.selectedMode
        } else {
            selectedMode
        }
        onSelectedModeChanged(selectedMode)
        timerText = if (mode == FlashlightMode.Off) null else getTimerText()
    }

    fun turnOn() {
        flashlight.set(selectedMode)
        refreshFlashlightState()
    }

    fun turnOff() {
        flashlight.set(FlashlightMode.Off)
        refreshFlashlightState()
    }

    fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            turnOff()
        } else {
            turnOn()
        }
    }

    fun updateBrightness(value: Float? = null) {
        val brightness = if (hasBrightnessControl) {
            value ?: prefs.flashlight.brightness
        } else {
            1f
        }
        brightnessValue = (brightness * maxBrightness).safeRoundToInt().toFloat()
        flashlight.setBrightness(brightness)
    }

    fun onDialSelectionChanged(selection: Int) {
        val isStrobe = selection in 1..10
        if (isStrobe) {
            CustomUiUtils.disclaimer(
                context,
                context.getString(R.string.strobe_warning_title),
                context.getString(R.string.strobe_warning_content),
                context.getString(R.string.pref_fine_with_strobe),
                considerShownIfCancelled = false,
            ) { _, agreed ->
                val frequency = if (selection == 10) 200 else selection
                selectedMode = if (agreed) {
                    getStrobeMode(frequency)
                } else {
                    FlashlightMode.Torch
                }
                onSelectedModeChanged(selectedMode)
                turnOn()
            }
        } else {
            selectedMode = when (selection) {
                11 -> FlashlightMode.Sos
                else -> FlashlightMode.Torch
            }
            onSelectedModeChanged(selectedMode)
            turnOn()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                    haptics.off()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            haptics.off()
        }
    }

    LaunchedEffect(isResumed) {
        if (!isResumed) {
            return@LaunchedEffect
        }

        updateBrightness()
        flashlightMode = flashlight.getMode()
        selectedMode = if (flashlightMode != FlashlightMode.Off) {
            flashlight.selectedMode
        } else {
            FlashlightMode.Torch
        }
        onSelectedModeChanged(selectedMode)
        timerText = if (flashlightMode == FlashlightMode.Off) null else getTimerText()

        while (isActive) {
            val mode = flashlight.getMode()
            if (mode != flashlightMode) {
                flashlightMode = mode
                if (mode != FlashlightMode.Off) {
                    selectedMode = flashlight.selectedMode
                    onSelectedModeChanged(selectedMode)
                }
            }
            timerText = if (mode == FlashlightMode.Off) null else getTimerText()
            delay(250)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .padding(TrailSenseSpacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.testTag("flashlight_btn_holder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
                if (hasFlashlight) {
                    PowerToggle(
                        on = flashlightMode != FlashlightMode.Off,
                        timerText = timerText,
                        onClick = ::toggle,
                        modifier = Modifier.testTag("flashlight_on_btn")
                    )
                }
                ScreenFlashlightCard(
                    onClick = onOpenScreenFlashlight,
                    modifier = Modifier.testTag("screen_flashlight_btn")
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                if (hasBrightnessControl) {
                    BrightnessCard(
                        value = brightnessValue,
                        maxBrightness = maxBrightness,
                        onValueChanged = {
                            brightnessValue = it
                            flashlight.setBrightness(it / maxBrightness.toFloat())
                            turnOn()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hasFlashlight) {
                    FlashlightModeCard(
                        selectedMode = selectedMode,
                        areHapticsEnabled = isResumed,
                        onSelectionChanged = ::onDialSelectionChanged,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        }
    }
}

/**
 * Large, satisfying circular toggle. When on it glows with the primary color and
 * a soft halo plus a gentle scale bump; when off it rests as a tonal surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PowerToggle(
    on: Boolean,
    timerText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val container by animateColorAsState(
        targetValue = if (on) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 250),
        label = "powerContainer"
    )
    val content by animateColorAsState(
        targetValue = if (on) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "powerContent"
    )
    val haloColor by animateColorAsState(
        targetValue = if (on) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "powerHalo"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            on -> 1.04f
            else -> 1f
        },
        animationSpec = TrailSenseAnimation.quickSpring,
        label = "powerScale"
    )

    Box(
        modifier = modifier
            .size(260.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing halo behind the button when on.
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(haloColor, CircleShape)
        )
        Surface(
            onClick = onClick,
            modifier = Modifier.size(220.dp),
            shape = CircleShape,
            color = container,
            contentColor = content,
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(TrailSenseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = stringResource(R.string.flashlight_title),
                    modifier = Modifier.size(96.dp),
                    tint = content
                )
                if (!timerText.isNullOrBlank()) {
                    Text(
                        text = timerText,
                        modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * A clean tonal pill that opens the screen-as-flashlight mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenFlashlightCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(260.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.xl,
                vertical = TrailSenseSpacing.lg
            ),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_screen_flashlight),
                        contentDescription = stringResource(R.string.screen_flashlight),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.screen_flashlight),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Brightness slider presented inside a labeled tonal card.
 */
@Composable
private fun BrightnessCard(
    value: Float,
    maxBrightness: Int,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    TrailSenseCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.brightness),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))
        BrightnessSlider(
            value = value,
            maxBrightness = maxBrightness,
            onValueChanged = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("brightness_seek")
        )
    }
}

@Composable
private fun BrightnessSlider(
    value: Float,
    maxBrightness: Int,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val brightnessMax = maxBrightness.coerceAtLeast(1)
    Slider(
        value = value.coerceIn(0f, brightnessMax.toFloat()),
        onValueChange = onValueChanged,
        modifier = modifier,
        valueRange = 0f..brightnessMax.toFloat(),
        steps = (brightnessMax - 1).coerceAtLeast(0)
    )
}

/**
 * Strobe / SOS mode controls grouped in a tonal card with a primary section
 * label and a row of selectable frequency chips.
 */
@Composable
private fun FlashlightModeCard(
    selectedMode: FlashlightMode,
    areHapticsEnabled: Boolean,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TrailSenseCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.frequency),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("flashlight_dial_indicator")
            )
        }
        Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
        FlashlightDial(
            selectedMode = selectedMode,
            areHapticsEnabled = areHapticsEnabled,
            onSelectionChanged = onSelectionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("flashlight_dial")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashlightDial(
    selectedMode: FlashlightMode,
    areHapticsEnabled: Boolean,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember { HapticSubsystem.getInstance(context) }
    val options = listOf(
        "0",
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "200",
        stringResource(R.string.sos)
    )
    val selectedIndex = getDialIndex(selectedMode)
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceIn(options.indices))
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(options) { index, option ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(durationMillis = 250),
                label = "dialContainer"
            )
            val itemContent by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(durationMillis = 250),
                label = "dialContent"
            )
            Surface(
                onClick = {
                    if (areHapticsEnabled) {
                        haptics.click()
                    }
                    if (!selected) {
                        onSelectionChanged(index)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("flashlight_dial_$index"),
                shape = CircleShape,
                color = container,
                contentColor = itemContent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun getDialIndex(mode: FlashlightMode): Int {
    return when (mode) {
        FlashlightMode.Strobe1 -> 1
        FlashlightMode.Strobe2 -> 2
        FlashlightMode.Strobe3 -> 3
        FlashlightMode.Strobe4 -> 4
        FlashlightMode.Strobe5 -> 5
        FlashlightMode.Strobe6 -> 6
        FlashlightMode.Strobe7 -> 7
        FlashlightMode.Strobe8 -> 8
        FlashlightMode.Strobe9 -> 9
        FlashlightMode.Strobe200 -> 10
        FlashlightMode.Sos -> 11
        else -> 0
    }
}

private fun getStrobeMode(frequency: Int): FlashlightMode {
    return when (frequency) {
        1 -> FlashlightMode.Strobe1
        2 -> FlashlightMode.Strobe2
        3 -> FlashlightMode.Strobe3
        4 -> FlashlightMode.Strobe4
        5 -> FlashlightMode.Strobe5
        6 -> FlashlightMode.Strobe6
        7 -> FlashlightMode.Strobe7
        8 -> FlashlightMode.Strobe8
        9 -> FlashlightMode.Strobe9
        200 -> FlashlightMode.Strobe200
        else -> FlashlightMode.Torch
    }
}
