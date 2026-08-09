@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

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
    view.keepScreenOn = true

    // The full-bleed light surface. The color animates between white and red so the
    // mode change feels intentional rather than abrupt.
    val lightColor by animateColorAsState(
        targetValue = if (isRedLight) Color.Red else Color.White,
        animationSpec = tween(durationMillis = 250),
        label = "flashlightSurface"
    )

    // Controls sit on a translucent dark scrim, so they stay legible whether the
    // background is bright white or saturated red.
    val onScrim = Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(lightColor)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(TrailSenseSpacing.lg)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Black.copy(alpha = 0.55f),
            contentColor = onScrim
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = TrailSenseSpacing.xl,
                    vertical = TrailSenseSpacing.lg
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brightness row: icon + label + slider, all on the scrim.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessHigh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = onScrim
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.labelLarge,
                        color = onScrim
                    )
                }

                Spacer(Modifier.height(TrailSenseSpacing.xs))

                Slider(
                    value = brightness,
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = onScrim,
                        activeTrackColor = onScrim,
                        inactiveTrackColor = onScrim.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(TrailSenseSpacing.md))

                // Action row: white/red color toggle alongside the primary turn-off action.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconToggleButton(
                        checked = isRedLight,
                        onCheckedChange = { onToggleColor() },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = onScrim,
                            checkedContainerColor = Color.Red,
                            checkedContentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null
                        )
                    }

                    Button(
                        onClick = onTurnOff,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(TrailSenseSpacing.sm))
                        Text(
                            text = stringResource(R.string.turn_off),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
