package com.kylecorry.andromeda.torch

import android.view.Window
import com.ksensor.core.context.KSensorContext

public actual fun screenBrightness(): ScreenBrightness? = KSensorContext.getActivity()?.window?.let(::screenBrightness)

public fun screenBrightness(window: Window): ScreenBrightness = object : ScreenBrightness {
    override var brightness: Float
        get() = window.attributes.screenBrightness
        set(value) {
            val attributes = window.attributes
            attributes.screenBrightness = value
            window.attributes = attributes
        }
}
