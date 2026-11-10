package com.kylecorry.trail_sense.main.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Whether Android offers wallpaper colors (Material 3's dynamicLightColorScheme / dynamicDarkColorScheme): Android 12
 * and later.
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun isDynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
