package com.kylecorry.trail_sense.shared.platform

import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * The platform's handle for UI work that leaves the app, like opening system settings. It is the Android Context;
 * other platforms have nothing to pass.
 */
expect abstract class PlatformContext

/** The [PlatformContext] of the current screen. */
expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>
