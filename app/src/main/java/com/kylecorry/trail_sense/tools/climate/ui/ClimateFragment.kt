package com.kylecorry.trail_sense.tools.climate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar

@Composable
fun Fragment.ClimateContent(modifier: Modifier = Modifier) {
    var locationText by remember { mutableStateOf("Current location") }
    var dateText by remember { mutableStateOf("Today") }
    var climateType by remember { mutableStateOf("Loading climate data...") }

    LaunchedEffect(Unit) {
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TrailSenseSearchBar(
            value = locationText,
            onValueChange = { locationText = it },
            placeholder = "Location"
        )

        TrailSenseSearchBar(
            value = dateText,
            onValueChange = { dateText = it },
            placeholder = "Date"
        )


        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(climateType, style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(8.dp))


        AndroidView(
            factory = { ctx ->
                android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_tool_climate, null)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

class ClimateFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ClimateContent()
    }
}

