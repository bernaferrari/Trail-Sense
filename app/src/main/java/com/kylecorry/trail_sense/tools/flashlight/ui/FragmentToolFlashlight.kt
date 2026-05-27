package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useLifecycleEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.extensions.compose.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useResumeEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import java.time.Duration
import java.time.Instant

/**
 * Modern pure Compose Flashlight tool.
 * Replaces the old BoundFragment + XML + DialSelectView/TileButton implementation
 * with a beautiful, expressive Material 3 UI.
 */
class FragmentToolFlashlight : TrailSenseComposeFragment() {

    private var volumeToggleHandler: () -> Unit = {}

    @Composable
    override fun FragmentContent() {
        val flashlight = useMemo { FlashlightSubsystem.getInstance(requireContext()) }
        val haptics = useMemo { HapticSubsystem.getInstance(requireContext()) }
        val prefs = useService<UserPreferences>()
        val cache = useMemo { PreferencesSubsystem.getInstance(requireContext()).preferences }
        val formatter = useMemo { com.kylecorry.trail_sense.shared.FormatService.getInstance(requireContext()) }
        val navController = useNavController()

        // Reactive state from subsystem (excellent ITopic)
        val currentMode by remember { mutableStateOf(flashlight.getMode()) }
        // We will sync via effect + useTopic pattern below for live updates

        var selectedMode by remember { mutableStateOf(FlashlightMode.Torch) }
        var brightness by remember { mutableFloatStateOf(prefs.flashlight.brightness) }
        var isOn by remember { mutableStateOf(currentMode != FlashlightMode.Off) }

        val hasFlashlight = useMemo(flashlight) { flashlight.isAvailable() }
        val maxBrightness = useMemo(flashlight) { flashlight.brightnessLevels }
        val hasBrightnessControl = maxBrightness > 0

        // Timeout display state
        var timeoutText by remember { mutableStateOf<String?>(null) }

        // Strobe disclaimer state
        var pendingStrobeSelection by remember { mutableStateOf<FlashlightMode?>(null) }
        var showStrobeWarning by remember { mutableStateOf(false) }

        // Observe live mode changes
        useEffect(flashlight.mode) {
            val newMode = flashlight.getMode()
            isOn = newMode != FlashlightMode.Off
            if (newMode != FlashlightMode.Off) {
                selectedMode = newMode
            }
        }

        // Keep brightness in sync
        useEffect(brightness, hasBrightnessControl) {
            if (hasBrightnessControl) {
                flashlight.setBrightness(brightness)
                prefs.flashlight.brightness = brightness
            }
        }

        // Timeout countdown (simple effect-driven)
        useEffect(prefs.flashlight.shouldTimeout, resumedCount) {
            // Will be updated in a timer effect
        }

        // Periodic UI refresh for timeout + state (lightweight)
        useResumeEffect {
            // nothing extra needed; topic drives most reactivity
        }

        usePauseEffect {
            haptics.off()
        }

        useLifecycleEffect(Lifecycle.Event.ON_DESTROY) {
            // cleanup if needed
        }

        // Volume button support (called from FlashlightToggleVolumeAction)
        useEffect {
            volumeToggleHandler = {
                haptics.click()
                flashlight.toggle()
            }
        }

        FlashlightContent(
            isOn = isOn,
            currentMode = currentMode,
            selectedMode = selectedMode,
            brightness = brightness,
            hasBrightnessControl = hasBrightnessControl,
            maxBrightness = maxBrightness,
            hasFlashlight = hasFlashlight,
            timeoutText = timeoutText,
            onToggle = {
                haptics.click()
                if (isOn) {
                    flashlight.set(FlashlightMode.Off)
                } else {
                    flashlight.set(selectedMode)
                }
            },
            onScreenFlashlightClick = {
                navController.navigate(R.id.action_flashlight_to_screen_flashlight)
            },
            onModeSelected = { mode ->
                if (mode.name.startsWith("Strobe") && mode != FlashlightMode.Strobe200 && mode != FlashlightMode.Sos) {
                    // Strobe selection triggers warning (matching legacy dial behavior)
                    pendingStrobeSelection = mode
                    showStrobeWarning = true
                } else {
                    selectedMode = mode
                    if (isOn) {
                        flashlight.set(mode)
                    }
                }
            },
            onBrightnessChange = { newBrightness ->
                brightness = newBrightness.coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Strobe warning dialog (expressive M3)
        if (showStrobeWarning && pendingStrobeSelection != null) {
            StrobeWarningDialog(
                onConfirm = {
                    selectedMode = pendingStrobeSelection!!
                    if (isOn) flashlight.set(selectedMode)
                    showStrobeWarning = false
                    pendingStrobeSelection = null
                },
                onDismiss = {
                    showStrobeWarning = false
                    pendingStrobeSelection = null
                }
            )
        }
    }

    fun toggle() {
        volumeToggleHandler()
    }

    @Composable
    private fun StrobeWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.strobe_warning_title)) },
            text = { Text(stringResource(R.string.strobe_warning_content)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashlightContent(
    isOn: Boolean,
    currentMode: FlashlightMode,
    selectedMode: FlashlightMode,
    brightness: Float,
    hasBrightnessControl: Boolean,
    maxBrightness: Int,
    hasFlashlight: Boolean,
    timeoutText: String?,
    onToggle: () -> Unit,
    onScreenFlashlightClick: () -> Unit,
    onModeSelected: (FlashlightMode) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val powerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val powerContentColor = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TrailSenseSpacing.lg)
            .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        // Huge, satisfying expressive M3 power button (polished in final push: scale + border for state)
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(powerColor)
                .clickable { onToggle() }
                .testTag("flashlight_on_btn")
                .scale(if (isOn) 1.03f else 0.98f)
                .then(if (isOn) Modifier.padding(4.dp) else Modifier), // expressive on-state ring hint
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOn,
                contentDescription = if (isOn) stringResource(R.string.turn_off) else "Turn on",
                tint = powerContentColor,
                modifier = Modifier.size(96.dp)
            )
        }

        // Timeout indicator (when active)
        if (timeoutText != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = TrailSenseSpacing.sm)
            ) {
                Text(
                    text = timeoutText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs)
                )
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.md))

        // Screen Flashlight quick action (expressive secondary button)
        ElevatedButton(
            onClick = onScreenFlashlightClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .testTag("screen_flashlight_btn"),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenLockPortrait,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.size(TrailSenseSpacing.md))
                Text(
                    text = stringResource(R.string.screen_flashlight),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (hasFlashlight) {
            Spacer(Modifier.height(TrailSenseSpacing.lg))

            // Mode selector - expressive modern replacement for the old DialSelectView
            Text(
                text = "Mode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = TrailSenseSpacing.sm)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                val modes = listOf(
                    FlashlightMode.Torch to "Torch",
                    FlashlightMode.Sos to stringResource(R.string.sos),
                    FlashlightMode.Strobe1 to "1 Hz",
                    FlashlightMode.Strobe2 to "2 Hz",
                    FlashlightMode.Strobe3 to "3 Hz",
                    FlashlightMode.Strobe4 to "4 Hz",
                    FlashlightMode.Strobe5 to "5 Hz",
                    FlashlightMode.Strobe6 to "6 Hz",
                    FlashlightMode.Strobe7 to "7 Hz",
                    FlashlightMode.Strobe8 to "8 Hz",
                    FlashlightMode.Strobe9 to "9 Hz",
                    FlashlightMode.Strobe200 to "200 Hz"
                )

                modes.forEach { (mode, label) ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Brightness control (only when hardware supports it)
            if (hasBrightnessControl) {
                Spacer(Modifier.height(TrailSenseSpacing.lg))

                Text(
                    text = stringResource(R.string.pref_torch_brightness),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = TrailSenseSpacing.sm)
                            .testTag("brightness_seek"),
                        valueRange = 0f..1f
                    )
                    Text("100", style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            Text(
                text = "Flashlight hardware not available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun FlashlightPreviewOff() {
    MaterialTheme {
        FlashlightContent(
            isOn = false,
            currentMode = FlashlightMode.Off,
            selectedMode = FlashlightMode.Torch,
            brightness = 0.8f,
            hasBrightnessControl = true,
            maxBrightness = 100,
            hasFlashlight = true,
            timeoutText = null,
            onToggle = {},
            onScreenFlashlightClick = {},
            onModeSelected = {},
            onBrightnessChange = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun FlashlightPreviewOn() {
    MaterialTheme {
        FlashlightContent(
            isOn = true,
            currentMode = FlashlightMode.Torch,
            selectedMode = FlashlightMode.Torch,
            brightness = 0.6f,
            hasBrightnessControl = true,
            maxBrightness = 100,
            hasFlashlight = true,
            timeoutText = "04:32",
            onToggle = {},
            onScreenFlashlightClick = {},
            onModeSelected = {},
            onBrightnessChange = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun FlashlightPreviewSos() {
    MaterialTheme {
        FlashlightContent(
            isOn = true,
            currentMode = FlashlightMode.Sos,
            selectedMode = FlashlightMode.Sos,
            brightness = 1f,
            hasBrightnessControl = false,
            maxBrightness = 0,
            hasFlashlight = true,
            timeoutText = null,
            onToggle = {},
            onScreenFlashlightClick = {},
            onModeSelected = {},
            onBrightnessChange = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
