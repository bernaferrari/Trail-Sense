package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.ScopeAdjustmentContent(modifier: Modifier = Modifier) {
    var adjustment by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                MaterialSpinnerView(ctx, null).apply {
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        AndroidView(
            factory = { ctx ->
                DistanceInputView(ctx, null).apply {
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = getString(R.string.scope_adjustment),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = adjustment ?: "",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

class FragmentScopeAdjustment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ScopeAdjustmentContent()
    }
}
