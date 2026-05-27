package com.kylecorry.trail_sense.tools.guide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

/**
 * Modern Compose implementation of guide detail screen (replaces XML + BoundFragment).
 * Uses expressive M3 cards, typography, and scroll. Markdown rendering approximated as plain text
 * for pure Compose (consistent with other migrated readers); full md view can use AndroidView if needed.
 */
@Composable
fun Fragment.GuideContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val name = remember { arguments?.getString("guide_name", "") ?: "" }
    val resource = remember { arguments?.getInt("guide_contents") ?: 0 }

    var content by remember { mutableStateOf("Loading guide...") }

    LaunchedEffect(resource) {
        if (resource != 0) {
            val loaded = onIO {
                TextUtils.loadTextFromResources(context, resource)
            }
            content = loaded.ifBlank { "No content available for this guide." }
        } else {
            content = "Guide not found."
        }
    }

    GuideContent(
        name = name,
        content = content,
        modifier = modifier
    )
}

@Composable
private fun GuideContent(
    name: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TrailSenseCard {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideContentPreview() {
    MaterialTheme {
        GuideContent(
            name = "How to Use the Compass",
            content = "The compass shows your current heading.\n\n" +
                    "• Calibrate regularly for accuracy.\n" +
                    "• Use true north or magnetic as needed.\n\n" +
                    "For more details, see the full documentation."
        )
    }
}

class GuideFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        GuideContent()
    }
}
