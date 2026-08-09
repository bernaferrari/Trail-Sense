package com.kylecorry.trail_sense.tools.guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun GuideScreen(name: String, resource: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sections by remember { mutableStateOf<List<TextSection>?>(null) }

    LaunchedEffect(resource) {
        sections = withContext(Dispatchers.IO) {
            val content = TextUtils.loadTextFromResources(context, resource)
            TextUtils.groupSections(TextUtils.getSections(content), null).flatten()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = name,
                modifier = Modifier
                    .padding(
                        start = TrailSenseSpacing.xl,
                        end = TrailSenseSpacing.xl,
                        top = TrailSenseSpacing.xl,
                        bottom = TrailSenseSpacing.md
                    )
                    .testTag("guide_name"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("guide_scroll")
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TrailSenseSpacing.xl)
                        .padding(bottom = TrailSenseSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    val loadedSections = sections
                    if (loadedSections == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(top = TrailSenseSpacing.md)
                                .align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        loadedSections.forEach {
                            GuideSection(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(section: TextSection) {
    section.title?.let {
        Text(
            text = it,
            style = when (section.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
    val body = TextUtils.removeMarkdownComments(section.content)
        .lineSequence()
        .map { it.cleanMarkdownLine() }
        .filter { it.isNotBlank() }
        .joinToString("\n")

    if (body.isNotBlank()) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
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
