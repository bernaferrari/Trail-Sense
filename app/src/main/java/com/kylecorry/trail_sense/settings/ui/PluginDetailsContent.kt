package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import kotlinx.coroutines.launch

@Composable
fun Fragment.PluginDetailsContent(modifier: Modifier = Modifier) {


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrailSenseCard {
            Text("Plugin Details (Pure Compose)", modifier = Modifier.padding(16.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
        }

        TrailSenseButton(text = "Open", onClick = {}, modifier = Modifier.fillMaxWidth())
        TrailSenseButton(text = "App Info", onClick = {}, modifier = Modifier.fillMaxWidth())
        TrailSenseButton(text = "Reload", onClick = {}, modifier = Modifier.fillMaxWidth())
        TrailSenseButton(text = "Disconnect", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

class PluginDetailsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PluginDetailsContent()
    }
}
