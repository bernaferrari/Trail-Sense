package com.kylecorry.trail_sense.tools.flashlight.ui

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class FragmentToolScreenFlashlight : TrailSenseComposeFragment() {

    private val flashlight by lazy { ScreenTorch(requireActivity().window) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private var brightness by mutableFloatStateOf(100f)
    private var isRedLight by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (cache.getBoolean("cache_red_light") == null) {
            cache.putBoolean("cache_red_light", false)
        }
        isRedLight = cache.getBoolean("cache_red_light") == true
        brightness = (cache.getInt(getString(R.string.pref_screen_torch_brightness)) ?: 100).toFloat()
    }

    @Composable
    override fun FragmentContent() {
        val view = LocalView.current
        view.keepScreenOn = true

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isRedLight) Color.Red else Color.White)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(TrailSenseSpacing.md)
                    .size(32.dp)
                    .background(if (isRedLight) Color.White else Color.Red)
                    .clickable { toggleColor() }
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
                    onValueChange = { setBrightness(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(TrailSenseSpacing.md))
                Button(
                    onClick = {
                        flashlight.off()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.turn_off),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    private fun toggleColor() {
        isRedLight = !isRedLight
        cache.putBoolean("cache_red_light", isRedLight)
    }

    private fun turnOn() {
        setBrightness(cache.getInt(getString(R.string.pref_screen_torch_brightness)) ?: 100)
    }

    private fun turnOff() {
        flashlight.off()
    }

    private fun setBrightness(percent: Int) {
        val coerced = percent.coerceIn(0, 100)
        brightness = coerced.toFloat()
        cache.putInt(getString(R.string.pref_screen_torch_brightness), coerced)
        flashlight.on(map(coerced / 100f, 0f, 1f, 0.1f, 1f))
    }

    fun increaseBrightness() {
        setBrightness((brightness.toInt() + 10).coerceAtMost(100))
    }

    fun decreaseBrightness() {
        setBrightness((brightness.toInt() - 10).coerceAtLeast(0))
    }

    fun handleVolumeButtonPress(isVolumeUp: Boolean) {
        if (isVolumeUp) {
            increaseBrightness()
        } else {
            decreaseBrightness()
        }
    }

    override fun onResume() {
        super.onResume()
        turnOn()
    }

    override fun onPause() {
        turnOff()
        super.onPause()
    }
}
