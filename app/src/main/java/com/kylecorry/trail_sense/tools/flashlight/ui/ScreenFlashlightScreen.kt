@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isRedLight) Color.Red else Color.White)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(TrailSenseSpacing.md)
                .size(32.dp)
                .background(if (isRedLight) Color.White else Color.Red)
                .clickable { onToggleColor() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = brightness,
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            Button(onClick = onTurnOff) {
                Text(
                    text = stringResource(R.string.turn_off),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
