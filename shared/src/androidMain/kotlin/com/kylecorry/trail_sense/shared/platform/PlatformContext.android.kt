package com.kylecorry.trail_sense.shared.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = android.content.Context

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalContext
