package com.kylecorry.trail_sense.tools.whistle.ui

import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.WhistleScreen

class ToolWhistleFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        WhistleScreen()
    }
}
