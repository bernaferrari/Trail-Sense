package com.kylecorry.trail_sense.shared.quickactions

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionOpenTool(
    button: QuickActionButtonView,
    fragment: TrailSenseDestinationHost,
    @IdRes private val navId: Int,
    @DrawableRes private val icon: Int
) :
    QuickActionButton(button, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(icon)
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().navigateWithAnimation(navId)
    }

}
