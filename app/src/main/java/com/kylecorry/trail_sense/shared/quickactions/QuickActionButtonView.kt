package com.kylecorry.trail_sense.shared.quickactions

import androidx.compose.ui.graphics.vector.ImageVector
import android.view.MotionEvent

/**
 * An abstraction over the widget backing a quick action button. This lets quick actions work with
 * Material buttons and Compose-hosted icon buttons without coupling actions to a specific UI toolkit.
 */
interface QuickActionButtonView {
    var isVisible: Boolean
    var contentDescription: CharSequence?

    fun setIcon(icon: ImageVector)
    fun setState(isOn: Boolean)
    fun setOnClickListener(listener: (() -> Unit)?)
    fun setOnLongClickListener(listener: (() -> Boolean)?)
    fun setOnTouchListener(listener: ((event: MotionEvent) -> Boolean)?)
}
