package com.kylecorry.trail_sense.shared.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment

@Composable
fun TrailSenseComposeNavHost(
    navigator: TrailSenseComposeNavigator,
    modifier: Modifier = Modifier
) {
    if (!navigator.isReady) {
        return
    }
    val backStack by navigator.backStack.collectAsState()
    val entry = backStack.lastOrNull() ?: return
    key(entry.id) {
        AndroidFragment<TrailSenseComposeDestinationFragment>(
            modifier = modifier,
            fragmentState = rememberFragmentState(),
            arguments = entry.arguments ?: Bundle()
        )
    }
}
