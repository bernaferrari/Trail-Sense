package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.volume.VolumeAction

data class ToolVolumeAction(
    val priority: ToolVolumeActionPriority,
    val isActive: (context: Context, isToolOpen: Boolean, host: AndroidDestinationHost) -> Boolean,
    val create: (host: AndroidDestinationHost) -> VolumeAction
)
