package com.kylecorry.trail_sense.tools.survival_guide.ui

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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader

@Composable
fun Fragment.SurvivalGuideReaderContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val chapterResourceId = remember { arguments?.getInt("chapter_resource_id") ?: 0 }

    var title by remember { mutableStateOf("Survival Guide") }
    var content by remember { mutableStateOf("Loading...") }

    LaunchedEffect(chapterResourceId) {
        val chapter = Chapters.getChapters(context).firstOrNull { it.resource == chapterResourceId }
        title = chapter?.title ?: "Survival Guide"

        if (chapter != null) {
            val loader = GuideLoader(context)
            val details = onIO { loader.load(chapter, includeContent = true) }
            val mainText = details.sections
                .mapNotNull { it.content }
                .joinToString("\n\n")
                .ifBlank { "Content not available for this chapter." }
            content = mainText
        } else {
            content = "Chapter not found."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

class FragmentToolSurvivalGuideReader : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SurvivalGuideReaderContent()
    }
}
