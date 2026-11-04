package com.kylecorry.trail_sense.tools.flashlight.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionScreenFlashlight(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ScreenFlashlight)
    }

    override fun onClick() {
        super.onClick()
        host.findNavController().navigateWithAnimation(R.id.fragmentToolScreenFlashlight)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().openTool(Tools.FLASHLIGHT)
        return true
    }
}
