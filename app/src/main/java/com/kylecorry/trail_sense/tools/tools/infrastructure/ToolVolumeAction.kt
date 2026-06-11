package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.volume.VolumeAction

data class ToolVolumeAction(
    val priority: ToolVolumeActionPriority,
    val isActive: (context: Context, isToolOpen: Boolean, host: TrailSenseDestinationHost) -> Boolean,
    val create: (host: TrailSenseDestinationHost) -> VolumeAction
)
