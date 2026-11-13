package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.andromeda.torch.screenBrightness

/** The screen flashlight on Android, where the light is the activity window's brightness. */
@Composable
fun ScreenFlashlightAndroidToolScreen(offerController: @Composable (Any) -> Unit = {}) {
    val window = requireNotNull(LocalActivity.current).window
    val light = remember(window) {
        val torch = ScreenTorch(screenBrightness(window))
        object : ScreenLight {
            override fun on(brightness: Float) = torch.on(brightness)
            override fun off() = torch.off()
        }
    }
    ScreenFlashlightToolScreen(light, offerController)
}
