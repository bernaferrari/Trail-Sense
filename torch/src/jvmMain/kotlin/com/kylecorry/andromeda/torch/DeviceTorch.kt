package com.kylecorry.andromeda.torch

actual fun deviceTorch(): Torch = UnavailableTorch

actual fun screenBrightness(): ScreenBrightness? = null

private object UnavailableTorch : Torch {
    override val brightnessLevels = 0
    override fun on() = Unit
    override fun on(brightness: Float) = Unit
    override fun off() = Unit
    override fun isAvailable() = false
    override fun isDimmable() = false
}
