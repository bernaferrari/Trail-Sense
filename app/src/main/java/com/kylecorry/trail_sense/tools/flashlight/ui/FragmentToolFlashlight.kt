package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant

class FragmentToolFlashlight : TrailSenseComposeFragment() {

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private val flashlight by lazy { FlashlightSubsystem.getInstance(requireContext()) }
    private var selectedMode = FlashlightMode.Torch

    @Composable
    override fun FragmentContent() {
        FlashlightScreen(
            initialSelectedMode = selectedMode,
            onSelectedModeChanged = { selectedMode = it },
            onOpenScreenFlashlight = {
                findNavController().navigate(R.id.action_flashlight_to_screen_flashlight)
            }
        )
    }

    fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            flashlight.set(FlashlightMode.Off)
        } else {
            flashlight.set(selectedMode)
        }
    }
}

@Composable
private fun FlashlightScreen(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.testTag("flashlight_btn_holder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasFlashlight) {
                    FlashlightTileButton(
                        icon = R.drawable.ic_power,
                        text = timerText,
                        selected = flashlightMode != FlashlightMode.Off,
                        size = 220.dp,
                        iconSize = 88.dp,
                        onClick = ::toggle,
                        modifier = Modifier.testTag("flashlight_on_btn")
                    )
                }
                FlashlightTileButton(
                    icon = R.drawable.ic_screen_flashlight,
                    text = stringResource(R.string.screen_flashlight),
                    selected = false,
                    width = 225.dp,
                    height = 102.dp,
                    iconSize = 36.dp,
                    onClick = onOpenScreenFlashlight,
                    modifier = Modifier.testTag("screen_flashlight_btn")
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (hasBrightnessControl) {
                BrightnessSlider(
                    value = brightnessValue,
                    maxBrightness = maxBrightness,
                    onValueChanged = {
                        brightnessValue = it
                        flashlight.setBrightness(it / maxBrightness.toFloat())
                        turnOn()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("brightness_seek")
                )
            }
            if (hasFlashlight) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp)
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("flashlight_dial_indicator")
                )
                FlashlightDial(
                    selectedMode = selectedMode,
                    areHapticsEnabled = isResumed,
                    onSelectionChanged = ::onDialSelectionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("flashlight_dial")
                )
            }
        }
    }
}

@Composable
private fun FlashlightTileButton(
    @DrawableRes icon: Int,
    text: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    width: Dp? = null,
    height: Dp? = null,
    iconSize: Dp = 48.dp
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val tileModifier = when {
        size != null -> modifier.size(size)
        width != null && height != null -> modifier.width(width).height(height)
        else -> modifier
    }

    Surface(
        modifier = tileModifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        contentColor = content
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = content
            )
            if (!text.isNullOrBlank()) {
                Text(
                    text = text,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                    textAlign = TextAlign.Center
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
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(options) { index, option ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .testTag("flashlight_dial_$index")
                    .clickable {
                        if (areHapticsEnabled) {
                            haptics.click()
                        }
                        if (!selected) {
                            onSelectionChanged(index)
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
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
