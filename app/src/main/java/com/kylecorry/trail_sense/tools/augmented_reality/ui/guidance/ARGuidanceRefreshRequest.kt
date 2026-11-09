package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import kotlin.time.Instant

import android.content.Context
import com.kylecorry.sol.units.Coordinate

data class ARGuidanceRefreshRequest(val context: Context, val location: Coordinate, val time: Instant)
