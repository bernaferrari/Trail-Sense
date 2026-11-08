package com.kylecorry.andromeda.torch

import android.view.Window

fun screenBrightness(window: Window): ScreenBrightness = object : ScreenBrightness {
    override var brightness: Float
        get() = window.attributes.screenBrightness
        set(value) {
            val attributes = window.attributes
            attributes.screenBrightness = value
            window.attributes = attributes
        }
}
