package com.kylecorry.trail_sense.tools.navigation.ui

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
 * Legacy Navigator (compass + sighting + map layers) fragment converted to thin
 * TrailSenseComposeFragment stub. Full modern navigation experience is the
 * AppRoute.Navigation destination (currently Placeholder, to be replaced with
 * expressive Compose implementation).
 *
 * Kept for:
 * - Legacy nav graph deep links / PendingIntents from widgets/notifications
 * - References to CACHE_CAMERA_ZOOM const from SightingCompassView
 * - Any volume or error handling callers
 */
class NavigatorFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        NavigatorContent(
            modifier = Modifier.fillMaxSize()
        )
    }

    companion object {
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
        // Other legacy constants removed during truncate (were private/internal).
    }
}

@Composable
private fun NavigatorContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TrailSenseEmptyState(
            title = stringResource(R.string.navigation),
            message = "The full compass, sighting compass, and navigation map are being rebuilt in pure Compose + M3 expressive design system.",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(TrailSenseSpacing.lg)
                .testTag("navigator_layout")
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigatorPreview() {
    MaterialTheme {
        NavigatorContent()
    }
}
