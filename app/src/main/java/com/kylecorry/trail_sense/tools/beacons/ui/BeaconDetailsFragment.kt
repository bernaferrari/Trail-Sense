package com.kylecorry.trail_sense.tools.beacons.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Legacy beacon details -> thin Compose wrapper for legacy nav graph / deep link support.
 * The original XML-backed implementation and binding code has been removed.
 */
class BeaconDetailsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        BeaconDetailsContent(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BeaconDetailsContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TrailSenseEmptyState(
            title = stringResource(R.string.beacon),
            message = "Full beacon details and actions (navigate, edit, share, delete) are available in the new Compose UI.",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(TrailSenseSpacing.lg)
                .testTag("beacon_title")
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BeaconDetailsPreview() {
    MaterialTheme {
        BeaconDetailsContent()
    }
}
