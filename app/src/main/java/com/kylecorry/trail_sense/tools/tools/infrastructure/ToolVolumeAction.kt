package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction

data class ToolVolumeAction(
    val priority: ToolVolumeActionPriority,
    val isActive: (context: Context, isToolOpen: Boolean, fragment: Fragment) -> Boolean,
    val create: (fragment: Fragment) -> VolumeAction
)
