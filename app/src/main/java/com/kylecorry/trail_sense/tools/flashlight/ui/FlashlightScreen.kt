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

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.flashlight_title),
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
            // Hero: the switch itself, with the auto-off countdown under it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xxl, bottom = TrailSenseSpacing.xl)
                    .testTag("flashlight_btn_holder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                PowerToggle(
                    on = isOn,
                    onClick = ::toggle,
                    modifier = Modifier.testTag("flashlight_on_btn")
                )
                // Always laid out so the controls below don't jump when the timer appears.
                Surface(
                    modifier = Modifier.alpha(if (timerText.isNullOrBlank()) 0f else 1f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = timerText?.let { stringResource(R.string.flashlight_turns_off_in, it) } ?: " ",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                    )
                }
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

    TrailSenseRowGroup {
        row { position ->
            TrailSenseGroupCard(position = position) {
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
            }
        }
        if (kind == FlashlightModeKind.Strobe) {
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(vertical = TrailSenseSpacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.flashlight_strobe_rate),
                        style = MaterialTheme.typography.bodyLarge,
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
                        style = MaterialTheme.typography.bodyLarge
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
 * The switch: a large cookie that blooms into a sun when the light comes on — the shape morphs, fills with the warm
 * tertiary tone, turns slowly and throws a soft glow behind it — and shrinks slightly under the finger.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PowerToggle(
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        targetValue = if (on) scheme.tertiary else scheme.surfaceContainerHigh,
        animationSpec = effects,
        label = "powerContainer"
    )
    val content by animateColorAsState(
        targetValue = if (on) scheme.onTertiary else scheme.onSurfaceVariant,
        animationSpec = effects,
        label = "powerContent"
    )
    val glow by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "powerGlow"
    )
    val bloom by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "powerBloom"
    )
    val spin by rememberInfiniteTransition(label = "sun").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunSpin"
    )
    val morph = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.VerySunny) }
    val shape = remember(bloom) { MorphShape(morph, bloom) }
    val stateText = stringResource(if (on) R.string.on else R.string.off)
    val glowColor = scheme.tertiary

    Box(
        modifier = modifier
            .size(300.dp)
            .trailSensePressScale(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (glow > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(alpha = 0.38f * glow), glowColor.copy(alpha = 0f)),
                        center = center,
                        radius = size.minDimension / 2f
                    )
                )
            }
        }
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(232.dp)
                .graphicsLayer { rotationZ = spin * bloom }
                .semantics { stateDescription = stateText },
            shape = shape,
            color = container,
            contentColor = content,
            interactionSource = interactionSource
        ) {}
        Icon(
            painter = rememberVectorPainter(if (on) TrailIcons.TorchOn else TrailIcons.TorchOff),
            contentDescription = stringResource(R.string.flashlight_title),
            modifier = Modifier.size(88.dp),
            tint = content
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

/**
 * No torch on this phone: the screen is the light. A one-line banner says why, and the screen light itself is the
 * hero, a big warm scalloped button. Tapping it blooms a warm-white glow out of the button until it fills the
 * screen, then hands over to the screen light.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoFlashlight(onOpenScreenFlashlight: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val bloom = remember { androidx.compose.animation.core.Animatable(0f) }
    var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var buttonCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val spin by rememberInfiniteTransition(label = "screenLight").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart),
        label = "screenLightSpin"
    )
    val warm = scheme.tertiary
    val glowStart = scheme.tertiaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.TorchOff),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(
                        text = stringResource(R.string.flashlight_unavailable_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.weight(0.8f))
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .trailSensePressScale(interactionSource),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(warm.copy(alpha = 0.30f), warm.copy(alpha = 0f)),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }
                Surface(
                    onClick = {
                        scope.launch {
                            bloom.animateTo(1f, tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            onOpenScreenFlashlight()
                            bloom.snapTo(0f)
                        }
                    },
                    modifier = Modifier
                        .size(208.dp)
                        .graphicsLayer { rotationZ = spin }
                        .onGloballyPositioned { buttonCenter = it.boundsInRoot().center }
                        .testTag("screen_flashlight_btn"),
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = scheme.tertiary,
                    contentColor = scheme.onTertiary,
                    interactionSource = interactionSource
                ) {}
                Icon(
                    painter = rememberVectorPainter(TrailIcons.ScreenFlashlight),
                    contentDescription = stringResource(R.string.flashlight_use_screen),
                    modifier = Modifier.size(84.dp),
                    tint = scheme.onTertiary
                )
            }
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            Text(
                text = stringResource(R.string.flashlight_use_screen),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(TrailSenseSpacing.sm))
            Text(
                text = stringResource(R.string.flashlight_unavailable_description),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1.2f))
        }
        // The bloom: warm light spreading from the button to the screen's edges.
        Canvas(Modifier.fillMaxSize()) {
            val p = bloom.value
            if (p > 0f) {
                val c = buttonCenter - origin
                val reach = size.maxDimension * 1.25f * p + 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.White,
                        0.7f to Color.White.copy(alpha = 0.96f),
                        1f to glowStart.copy(alpha = 0f),
                        center = c,
                        radius = reach
                    ),
                    radius = reach,
                    center = c
                )
            }
        }
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
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = effects,
                label = "strobeContainer"
            )
            val itemContent by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
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
