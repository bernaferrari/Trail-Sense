@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.flashlight.ui

import com.kylecorry.trail_sense.shared.views.compose.KeepScreenOn

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.preferences.IPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

/**
 * The screen as a light: the whole screen is the lamp (white or red), with one dark floating panel at the bottom for
 * brightness, color and turning it off. No top bar; the light is the point, and back still works.
 */
@Composable
fun ScreenFlashlightScreen(
    brightness: Float,
    isRedLight: Boolean,
    onBrightnessChange: (Int) -> Unit,
    onToggleColor: () -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val lightColor by animateColorAsState(
        targetValue = if (isRedLight) Color.Red else Color.White,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "flashlightSurface"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(lightColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        // A dark panel stays legible over both a white and a red light.
        Surface(
            modifier = Modifier
                .padding(TrailSenseSpacing.lg)
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .fillMaxWidth(),
            shape = RoundedCornerShape(TrailSenseShape.groupOuter),
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(TrailSenseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${brightness.roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brightness_seek"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                TrailSenseConnectedChoice(
                    options = listOf(
                        TrailSenseChoice(false, stringResource(R.string.screen_flashlight_white)),
                        TrailSenseChoice(true, stringResource(R.string.screen_flashlight_red))
                    ),
                    selected = isRedLight,
                    onSelect = { onToggleColor() },
                    label = stringResource(R.string.color),
                    modifier = Modifier.testTag("red_white_switcher")
                )
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.turn_off),
                    onClick = onTurnOff,
                    icon = Icons.Rounded.PowerSettingsNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("off_btn")
                )
            }
        }
    }
}

/** The screen flashlight: lit while the screen is resumed, and the volume buttons step its brightness. */
@Composable
fun ScreenFlashlightToolScreen(offerController: @Composable (Any) -> Unit = {}) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    // The screen torch drives the activity's window brightness.
    val window = requireNotNull(LocalActivity.current).window
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val state = remember(window) {
        ScreenFlashlightState(
            torch = ScreenTorch(window),
            cache = PreferencesSubsystem.getInstance(context).preferences,
            brightnessKey = context.getString(R.string.pref_screen_torch_brightness)
        )
    }
    offerController(state)

    DisposableEffect(lifecycleOwner, state) {
        state.load()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.turnOn()
                Lifecycle.Event.ON_PAUSE -> state.turnOff()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ScreenFlashlightScreen(
        brightness = state.brightness,
        isRedLight = state.isRedLight,
        onBrightnessChange = state::setBrightness,
        onToggleColor = state::toggleColor,
        onTurnOff = {
            state.turnOff()
            dispatcher?.onBackPressed()
        }
    )
}

private class ScreenFlashlightState(
    private val torch: ScreenTorch,
    private val cache: IPreferences,
    private val brightnessKey: String
) : ScreenFlashlightController {

    var brightness by mutableFloatStateOf(100f)
        private set
    var isRedLight by mutableStateOf(false)
        private set

    fun load() {
        if (cache.getBoolean(RED_LIGHT_KEY) == null) {
            cache.putBoolean(RED_LIGHT_KEY, false)
        }
        isRedLight = cache.getBoolean(RED_LIGHT_KEY) == true
        brightness = (cache.getInt(brightnessKey) ?: 100).toFloat()
    }

    fun toggleColor() {
        isRedLight = !isRedLight
        cache.putBoolean(RED_LIGHT_KEY, isRedLight)
    }

    fun turnOn() {
        setBrightness(cache.getInt(brightnessKey) ?: 100)
    }

    fun turnOff() {
        torch.off()
    }

    fun setBrightness(percent: Int) {
        val coerced = percent.coerceIn(0, 100)
        brightness = coerced.toFloat()
        cache.putInt(brightnessKey, coerced)
        torch.on(map(coerced / 100f, 0f, 1f, 0.1f, 1f))
    }

    override fun handleVolumeButtonPress(isVolumeUp: Boolean) {
        if (isVolumeUp) {
            setBrightness((brightness.toInt() + 10).coerceAtMost(100))
        } else {
            setBrightness((brightness.toInt() - 10).coerceAtLeast(0))
        }
    }

    private companion object {
        const val RED_LIGHT_KEY = "cache_red_light"
    }
}
