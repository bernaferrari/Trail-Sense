package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable

/**
 * Whether the user lets decorative animations run: false when Android's animator duration scale is 0 or iOS's
 * Reduce Motion is on.
 */
@Composable
expect fun rememberSystemAnimationsEnabled(): Boolean
