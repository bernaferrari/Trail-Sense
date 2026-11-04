package com.kylecorry.trail_sense.shared.quickactions

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionOpenTool(
    button: QuickActionButtonView,
    host: TrailSenseDestinationHost,
    @IdRes private val navId: Int,
    private val icon: ImageVector
) :
    QuickActionButton(button, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(icon)
    }

    override fun onClick() {
        super.onClick()
        host.findNavController().navigateWithAnimation(navId)
    }

}
