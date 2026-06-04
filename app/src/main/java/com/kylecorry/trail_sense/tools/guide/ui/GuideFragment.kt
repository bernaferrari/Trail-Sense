package com.kylecorry.trail_sense.tools.guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GuideFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        GuideScreen(
            name = requireArguments().getString("guide_name", ""),
            resource = requireArguments().getInt("guide_contents")
        )
    }
}

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
                    .padding(16.dp)
                    .testTag("guide_name"),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("guide_scroll")
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val loadedSections = sections
                    if (loadedSections == null) {
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
