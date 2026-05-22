package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import kotlinx.coroutines.delay
import java.time.Duration

@Composable
fun Fragment.SignalFinderContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = useNavController()
    val markdown = remember { MarkdownService.getInstance(context) }
    val navigator = remember { Navigator.getInstance(context) }

    var signals by remember { mutableStateOf(emptyList<com.kylecorry.andromeda.signal.CellSignal>()) }
    var nearby by remember { mutableStateOf(emptyList<ApproximateCoordinate>()) }
    var loading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<com.kylecorry.sol.units.Coordinate?>(null) }

    LaunchedEffect(Unit) {
        delay(100)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = if (loading) getString(R.string.loading) else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        TrailSenseCard {
            Text(
                text = markdown.toMarkdown(getString(R.string.cell_tower_disclaimer)),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))

        AndroidView(
            factory = { ctx ->
                AndromedaListView(ctx).apply {
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
    }
}

class ToolSignalFinderFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SignalFinderContent()
    }
}
