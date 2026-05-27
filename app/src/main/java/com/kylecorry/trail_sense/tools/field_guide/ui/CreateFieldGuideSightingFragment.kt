package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.CreateFieldGuideSightingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text("Create Sighting", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { }) {
                    Text("Save")
                }
            }
        }
    }
}

class CreateFieldGuideSightingFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        CreateFieldGuideSightingContent()
    }
}
