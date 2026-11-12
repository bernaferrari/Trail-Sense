package com.kylecorry.trail_sense.shared.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual abstract class PlatformContext private constructor() {
    companion object Instance : PlatformContext()
}

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { PlatformContext.Instance }
