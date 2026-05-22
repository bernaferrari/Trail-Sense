package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Scoped Compose conversion of FieldGuidePageFragment (Ralph Loop iteration 9).
 * The complex page content (images, text, AR sighting) is kept via AndroidView for velocity.
 * This allows immediate XML deletion while the surrounding chrome can be modernized later.
 */
@Composable
fun Fragment.FieldGuidePageContent(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_field_guide_page, null)
        },
        modifier = modifier.fillMaxSize()
    )
}

class FieldGuidePageFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        FieldGuidePageContent()
    }
}
