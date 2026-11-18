package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.registerSkikoComposeImplementation

@OptIn(InternalComposeUiApi::class)
internal actual fun prepareTestGraphics() {
    registerSkikoComposeImplementation()
}
