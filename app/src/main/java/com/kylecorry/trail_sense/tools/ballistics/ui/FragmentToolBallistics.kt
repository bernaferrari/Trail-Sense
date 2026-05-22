package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

class FragmentToolBallistics : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        AndroidView(
            factory = { ctx ->
                android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_tabs, null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
