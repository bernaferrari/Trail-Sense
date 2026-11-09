package com.kylecorry.andromeda.torch

/** Returns screen brightness control when the platform has an active screen. */
expect fun screenBrightness(): ScreenBrightness?

/** Platform screen brightness control; a negative initial value can represent the system default. */
interface ScreenBrightness {
    var brightness: Float
}

class ScreenTorch(private val screen: ScreenBrightness) : Torch {
    private var previousBrightness: Float? = null
    override val brightnessLevels: Int = 255
    override fun isAvailable(): Boolean = true
    override fun isDimmable(): Boolean = true
    override fun on() = on(1f)
    override fun on(brightness: Float) {
        if (previousBrightness == null) previousBrightness = screen.brightness
        screen.brightness = brightness.coerceIn(0f, 1f)
    }
    override fun off() {
        previousBrightness?.let { screen.brightness = it }
        previousBrightness = null
    }
}
