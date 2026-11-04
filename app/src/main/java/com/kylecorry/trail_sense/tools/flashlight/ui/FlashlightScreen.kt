@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.KeyboardArrowRight
import com.kylecorry.trail_sense.shared.icons.trail.Power
import com.kylecorry.trail_sense.shared.icons.trail.ScreenFlashlight
import com.kylecorry.trail_sense.shared.icons.trail.Strobe
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import com.kylecorry.trail_sense.shared.icons.trail.WhistleSos
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.Morph
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.graphics.shapes.toPath
import com.kylecorry.trail_sense.shared.icons.trail.TorchOff
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState

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

    val isOn = flashlightMode != FlashlightMode.Off

    // One question: is the light on?
    // The same medium title as the family's other tools.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.flashlight_title),
        largeTitle = true,
        modifier = modifier
    ) {
        if (!hasFlashlight) {
            NoFlashlight(onOpenScreenFlashlight)
            return@TrailSenseInstrumentScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrailSenseSpacing.lg)
                .padding(bottom = TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The main action: the switch itself, with the auto-off countdown under it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.xl)
                    .testTag("flashlight_btn_holder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                PowerToggle(
                    on = isOn,
                    kind = selectedMode.kind(),
                    strobeHz = strobeHz(selectedMode),
                    brightness = if (hasBrightnessControl && maxBrightness > 0) {
                        (brightnessValue / maxBrightness).coerceIn(0f, 1f)
                    } else {
                        1f
                    },
                    onClick = ::toggle,
                    modifier = Modifier.testTag("flashlight_on_btn")
                )
                // Always laid out so the controls below don't jump when the timer appears.
                Text(
                    text = timerText?.let { stringResource(R.string.flashlight_turns_off_in, it) } ?: " ",
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (timerText.isNullOrBlank()) 0f else 1f)
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth()
                    .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                FlashlightControls(
                    selectedMode = selectedMode,
                    areHapticsEnabled = isResumed,
                    hasBrightnessControl = hasBrightnessControl,
                    brightnessValue = brightnessValue,
                    maxBrightness = maxBrightness,
                    onBrightnessChanged = {
                        brightnessValue = it
                        flashlight.setBrightness(it / maxBrightness.toFloat())
                        turnOn()
                    },
                    onDialSelectionChanged = ::onDialSelectionChanged,
                    onOpenScreenFlashlight = onOpenScreenFlashlight
                )
            }
        }
    }
}

private enum class FlashlightModeKind { Torch, Strobe, Sos }

private fun FlashlightMode.kind(): FlashlightModeKind {
    return when (getDialIndex(this)) {
        in 1..10 -> FlashlightModeKind.Strobe
        11 -> FlashlightModeKind.Sos
        else -> FlashlightModeKind.Torch
    }
}

/** Mode, strobe rate (only while strobing), brightness and the screen light, as one group. */
@Composable
private fun FlashlightControls(
    selectedMode: FlashlightMode,
    areHapticsEnabled: Boolean,
    hasBrightnessControl: Boolean,
    brightnessValue: Float,
    maxBrightness: Int,
    onBrightnessChanged: (Float) -> Unit,
    onDialSelectionChanged: (Int) -> Unit,
    onOpenScreenFlashlight: () -> Unit
) {
    val context = LocalContext.current
    val haptics = remember { HapticSubsystem.getInstance(context) }
    val kind = selectedMode.kind()
    val choices = listOf(
        TrailSenseChoice(
            FlashlightModeKind.Torch,
            stringResource(R.string.flashlight_mode_steady),
            TrailIcons.TorchOn
        ),
        TrailSenseChoice(
            FlashlightModeKind.Strobe,
            stringResource(R.string.flashlight_mode_strobe),
            TrailIcons.Strobe
        ),
        TrailSenseChoice(
            FlashlightModeKind.Sos,
            stringResource(R.string.sos),
            TrailIcons.WhistleSos
        )
    )
    val modeLabel = stringResource(R.string.flashlight_mode)

    // The mode is the one choice row, on the page itself (not boxed in a card); the rest is one grouped list.
    TrailSenseConnectedChoice(
        options = choices,
        selected = kind,
        onSelect = {
            if (areHapticsEnabled) {
                haptics.click()
            }
            onDialSelectionChanged(
                when (it) {
                    FlashlightModeKind.Torch -> 0
                    FlashlightModeKind.Strobe -> 1
                    FlashlightModeKind.Sos -> 11
                }
            )
        },
        modifier = Modifier.testTag("flashlight_dial"),
        label = modeLabel
    )
    Spacer(Modifier.height(TrailSenseSpacing.lg))
    TrailSenseRowGroup {
        if (kind == FlashlightModeKind.Strobe) {
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(vertical = TrailSenseSpacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.flashlight_strobe_rate),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(
                            horizontal = TrailSenseSpacing.lg,
                            vertical = TrailSenseSpacing.xs
                        )
                    )
                    StrobeFrequencies(
                        selectedMode = selectedMode,
                        areHapticsEnabled = areHapticsEnabled,
                        onSelectionChanged = onDialSelectionChanged
                    )
                }
            }
        }
        if (hasBrightnessControl) {
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.md,
                        bottom = TrailSenseSpacing.xs
                    )
                ) {
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.titleMedium
                    )
                    BrightnessSlider(
                        value = brightnessValue,
                        maxBrightness = maxBrightness,
                        onValueChanged = onBrightnessChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brightness_seek")
                    )
                }
            }
        }
        row { position ->
            TrailSenseListItem(
                title = stringResource(R.string.flashlight_use_screen),
                subtitle = stringResource(R.string.flashlight_use_screen_summary),
                icon = rememberVectorPainter(TrailIcons.ScreenFlashlight),
                position = position,
                onClick = onOpenScreenFlashlight,
                trailing = {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("screen_flashlight_btn")
            )
        }
    }
}

/**
 * The lamp: a large cookie that rounds into a sun when the light comes on (the shape is the state) and fills with
 * primary. Lit, it throws a glow that grows with the brightness, flashes at the strobe rate and spells SOS in SOS
 * mode, so the screen shows what the torch is doing. Off, it is a quiet unlit bulb, not a crossed-out one.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PowerToggle(
    on: Boolean,
    kind: FlashlightModeKind,
    strobeHz: Int,
    brightness: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        targetValue = if (on) scheme.primary else scheme.surfaceContainerHigh,
        animationSpec = effects,
        label = "powerContainer"
    )
    val content by animateColorAsState(
        targetValue = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = effects,
        label = "powerContent"
    )
    val bloom by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "powerBloom"
    )
    val glowSize by animateFloatAsState(
        targetValue = if (on) 0.55f + 0.45f * brightness else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "glowSize"
    )
    // How lit the lamp is right now: steady, the strobe's on/off beat, or the SOS pattern.
    var pulse by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(on, kind, strobeHz) {
        pulse = 1f
        if (!on || kind == FlashlightModeKind.Torch || (kind == FlashlightModeKind.Strobe && strobeHz > 20)) {
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { now ->
                val elapsed = now - start
                pulse = when (kind) {
                    FlashlightModeKind.Strobe -> if ((elapsed * strobeHz * 2 / 1000) % 2 == 0L) 1f else 0.12f
                    else -> if (sosLit(elapsed)) 1f else 0.12f
                }
            }
        }
    }
    val glowColor = scheme.primary
    val morph = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.VerySunny) }
    val shape = remember(bloom) { MorphShape(morph, bloom) }
    val stateText = stringResource(if (on) R.string.on else R.string.off)

    Box(
        modifier = modifier
            .size(260.dp)
            .trailSensePressScale(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        // The light it throws, behind the lamp.
        Canvas(Modifier.fillMaxSize()) {
            if (glowSize > 0f) {
                val radius = size.minDimension / 2f * (0.9f + 0.45f * glowSize)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.55f * glowSize * pulse),
                            glowColor.copy(alpha = 0.18f * glowSize * pulse),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
            }
        }
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(232.dp)
                .graphicsLayer { alpha = if (on) 0.7f + 0.3f * pulse else 1f }
                .semantics { stateDescription = stateText },
            shape = shape,
            color = container,
            contentColor = content,
            interactionSource = interactionSource
        ) {}
        Icon(
            painter = rememberVectorPainter(TrailIcons.TorchOn),
            contentDescription = stringResource(R.string.flashlight_title),
            modifier = Modifier.size(88.dp),
            tint = if (on) content else content.copy(alpha = 0.6f)
        )
    }
}

/** A shape that is part way between two Material shapes, for the switch's cookie-to-sun bloom. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = morph.toPath(progress)
        val matrix = android.graphics.Matrix().apply { setScale(size.width, size.height) }
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

/** No torch on this phone: a calm empty state whose one action opens the screen light. */
@Composable
private fun NoFlashlight(onOpenScreenFlashlight: () -> Unit) {
    TrailSenseEmptyState(
        icon = TrailIcons.TorchOff,
        // The headline says what's missing; the button says what to do instead, so no sentence repeats it.
        title = stringResource(R.string.flashlight_unavailable_title),
        actionLabel = stringResource(R.string.flashlight_use_screen_action),
        actionIcon = TrailIcons.ScreenFlashlight,
        onAction = onOpenScreenFlashlight,
        actionModifier = Modifier.testTag("screen_flashlight_btn"),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun BrightnessSlider(
    value: Float,
    maxBrightness: Int,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val brightnessMax = maxBrightness.coerceAtLeast(1)
    // One plain handle without step dots; the value still snaps to the torch's own levels.
    Slider(
        value = value.coerceIn(0f, brightnessMax.toFloat()),
        onValueChange = { onValueChanged(it.safeRoundToInt().toFloat()) },
        modifier = modifier,
        valueRange = 0f..brightnessMax.toFloat()
    )
}

/** Strobe rates in flashes per second: 1 to 9, then 200 (a near-steady flicker). */
@Composable
private fun StrobeFrequencies(
    selectedMode: FlashlightMode,
    areHapticsEnabled: Boolean,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember { HapticSubsystem.getInstance(context) }
    val options = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "200")
    val selectedIndex = getDialIndex(selectedMode)
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in 1..10) {
            listState.animateScrollToItem(selectedIndex - 1)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(options) { offset, option ->
            val index = offset + 1
            val selected = index == selectedIndex
            val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = effects,
                label = "strobeContainer"
            )
            val itemContent by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = effects,
                label = "strobeContent"
            )
            val interactionSource = remember { MutableInteractionSource() }
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
                    .size(TrailSenseSize.minimumTouchTarget)
                    .trailSensePressScale(interactionSource)
                    .testTag("flashlight_dial_$index"),
                shape = CircleShape,
                color = container,
                contentColor = itemContent,
                interactionSource = interactionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Flashes per second for a strobe mode (0 otherwise). */
private fun strobeHz(mode: FlashlightMode): Int {
    return when (val index = getDialIndex(mode)) {
        in 1..9 -> index
        10 -> 200
        else -> 0
    }
}

/** S-O-S in Morse units of 200 ms: three short, three long, three short, then a long pause. */
private val SOS_PATTERN: BooleanArray = run {
    val units = mutableListOf<Boolean>()
    fun mark(length: Int) {
        repeat(length) { units.add(true) }
        units.add(false)
    }
    repeat(3) { mark(1) }
    repeat(2) { units.add(false) }
    repeat(3) { mark(3) }
    repeat(2) { units.add(false) }
    repeat(3) { mark(1) }
    repeat(6) { units.add(false) }
    units.toBooleanArray()
}

private fun sosLit(elapsedMillis: Long): Boolean {
    return SOS_PATTERN[((elapsedMillis / 200) % SOS_PATTERN.size).toInt()]
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
