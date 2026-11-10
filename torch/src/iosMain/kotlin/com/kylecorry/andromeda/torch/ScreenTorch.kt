package com.kylecorry.andromeda.torch

import platform.UIKit.UIScreen

public actual fun screenBrightness(): ScreenBrightness? = object : ScreenBrightness {
    override var brightness: Float
        get() = UIScreen.mainScreen.brightness.toFloat()
        set(value) { UIScreen.mainScreen.brightness = value.coerceIn(0f, 1f).toDouble() }
}
