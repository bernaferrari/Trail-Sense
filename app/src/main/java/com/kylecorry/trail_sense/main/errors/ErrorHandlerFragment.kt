package com.kylecorry.trail_sense.main.errors

import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Error handler screen — now on Compose (Ralph Loop).
 */
class ErrorHandlerFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ErrorHandlerContent()
    }
}
