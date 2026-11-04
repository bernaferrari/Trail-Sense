package com.kylecorry.trail_sense.tools.guide.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One user guide as a calm reading page: the guide's name is the page title, then each section is a heading and its
 * text, selectable for copying.
 */
@Composable
internal fun GuideScreen(name: String, resource: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var sections by remember { mutableStateOf<List<TextSection>?>(null) }

    LaunchedEffect(resource) {
        sections = withContext(Dispatchers.IO) {
            val content = TextUtils.loadTextFromResources(context, resource)
            TextUtils.groupSections(TextUtils.getSections(content), null).flatten()
        }
    }

    TrailSensePage(
        title = name,
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        modifier = Modifier.testTag("guide_scroll")
    ) {
        val loadedSections = sections
        if (loadedSections == null) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            loadedSections.forEachIndexed { index, section ->
                item(key = index) {
                    SelectionContainer {
                        GuideSection(section)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(section: TextSection) {
    val body = remember(section) {
        TextUtils.removeMarkdownComments(section.content)
            .lineSequence()
            .map { it.cleanMarkdownLine() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
    if (section.title == null && body.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        section.title?.let {
            Text(
                text = it,
                style = when (section.level) {
                    1, 2 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.cleanMarkdownLine(): String {
    return trim()
        .removePrefix("### ")
        .removePrefix("## ")
        .removePrefix("# ")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""__(.*?)__"""), "$1")
        .replace(Regex("""\[(.*?)]\((.*?)\)"""), "$1")
}
