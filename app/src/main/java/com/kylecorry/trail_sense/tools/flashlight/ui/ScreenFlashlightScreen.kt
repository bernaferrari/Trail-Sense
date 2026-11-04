@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.flashlight.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalView
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
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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
                .navigationBarsPadding()
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
                        style = MaterialTheme.typography.bodyLarge,
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
                val interactionSource = remember { MutableInteractionSource() }
                Button(
                    onClick = onTurnOff,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .trailSensePressScale(interactionSource, 0.985f)
                        .testTag("off_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.turn_off),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
