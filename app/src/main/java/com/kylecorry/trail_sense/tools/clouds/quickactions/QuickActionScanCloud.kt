package com.kylecorry.trail_sense.tools.clouds.quickactions

import android.os.Bundle
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionScanCloud(button: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(button, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.cloud_scanner)
    }

    override fun onClick() {
        super.onClick()
        host.findNavController().navigateWithAnimation(
            R.id.cloudFragment,
            Bundle().apply {
                putBoolean("open_scanner", true)
            },
        )
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigateWithAnimation(
            R.id.cloudFragment
        )
        return true
    }


}
