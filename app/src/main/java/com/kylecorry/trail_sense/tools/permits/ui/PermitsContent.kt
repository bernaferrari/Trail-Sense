package com.kylecorry.trail_sense.tools.permits.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Full Compose conversion of FragmentToolPermits (Ralph Loop iteration 14).
 * Tiny screen — perfect for a clean, pretty conversion.
 */
@Composable
fun Fragment.PermitsContent(modifier: Modifier = Modifier) {
    // Placeholder for a pretty permits list.
    // In a full conversion we would use a LazyColumn with TrailSenseListItem or cards.
    Text(
        text = "Permits (pretty Compose version coming soon)",
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}

class FragmentToolPermits : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PermitsContent()
    }
}
