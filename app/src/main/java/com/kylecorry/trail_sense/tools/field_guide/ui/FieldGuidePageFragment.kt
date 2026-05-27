package com.kylecorry.trail_sense.tools.field_guide.ui

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
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTagRow
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

@Composable
fun Fragment.FieldGuidePageContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val repo = remember { FieldGuideRepo.getInstance(context) }
    val pageId = remember { arguments?.getLong("page_id") ?: 0L }

    var page by remember { mutableStateOf<FieldGuidePage?>(null) }

    LaunchedEffect(pageId) {
        page = repo.getPage(pageId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = page?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall
                )

                page?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Spacer(Modifier.height(8.dp))
                    TrailSenseTagRow(tags = tags.map { it.name })
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = page?.notes ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

class FieldGuidePageFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        FieldGuidePageContent()
    }
}
