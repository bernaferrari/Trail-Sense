package com.kylecorry.trail_sense.tools.declination.ui

import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * ToolDeclinationFragment — now on Compose (Ralph Loop).
 * The heavy custom views are wrapped via AndroidView inside DeclinationContent.
 */
class ToolDeclinationFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        DeclinationContent()
    }
}
