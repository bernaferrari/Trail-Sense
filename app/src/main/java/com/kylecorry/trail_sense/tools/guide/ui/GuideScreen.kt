@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kylecorry.trail_sense.tools.guide.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.OpenBook
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One user guide as an article: a book-shaped hero with the reading time, a contents strip that jumps to a section,
 * the introduction, then each section under a numbered heading in reading typography. Text is selectable.
 */
@Composable
internal fun GuideScreen(name: String, resource: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var sections by remember { mutableStateOf<List<TextSection>?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(resource) {
        sections = withContext(Dispatchers.IO) {
            val content = TextUtils.loadTextFromResources(context, resource)
            TextUtils.groupSections(TextUtils.getSections(content), null).flatten()
        }
    }

    // One question: how does this tool work? The guide's text answers it, as an article with its contents up front.
    TrailSensePage(
        title = name,
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        state = listState,
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
                    LoadingIndicator()
                }
            }
        } else {
            val visible = loadedSections.filter { it.title != null || it.content.isNotBlank() }
            val words = visible.sumOf { it.content.split(Regex("\\s+")).size }
            val major = visible.filter { it.title != null && (it.level ?: 2) <= 2 }
            item(key = "hero") {
                GuideHero(
                    title = name,
                    minutes = (words / 200).coerceAtLeast(1),
                    sections = major.size
                )
            }
            if (major.size >= 3) {
                item(key = "contents") {
                    GuideContents(major.mapNotNull { it.title }) { index ->
                        val target = visible.indexOf(major[index])
                        // Hero and contents come first.
                        scope.launch { listState.animateScrollToItem(target + 2) }
                    }
                }
            }
            var number = 0
            visible.forEachIndexed { index, section ->
                val isMajor = section.title != null && (section.level ?: 2) <= 2
                if (isMajor) number++
                val sectionNumber = number
                item(key = index) {
                    SelectionContainer {
                        GuideSection(section, if (isMajor) sectionNumber else null)
                    }
                }
            }
        }
    }
}

/** The top bar already names the guide, so its top is one quiet line: how long it takes and how many sections. */
@Composable
private fun GuideHero(@Suppress("UNUSED_PARAMETER") title: String, minutes: Int, sections: Int) {
    Text(
        listOfNotNull(
            stringResource(R.string.sky_minutes_read, minutes),
            if (sections > 0) pluralSections(sections) else null
        ).joinToString(" · "),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
    )
}

@Composable
private fun pluralSections(count: Int): String =
    androidx.compose.ui.res.pluralStringResource(R.plurals.sky_sections, count, count)

/** The guide's sections as tappable chips, numbered like their headings. */
@Composable
private fun GuideContents(titles: List<String>, onSelect: (Int) -> Unit) {
    Column {
        Text(
            stringResource(R.string.sky_in_this_guide),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.sm)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            titles.forEachIndexed { index, title ->
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = TrailSenseSpacing.sm)
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(section: TextSection, number: Int?) {
    val body = remember(section) { TextUtils.removeMarkdownComments(section.content).trim() }
    if (section.title == null && body.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = TrailSenseSpacing.xs,
                end = TrailSenseSpacing.xs,
                top = if (number != null) TrailSenseSpacing.md else 0.dp
            ),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        section.title?.let {
            if (number != null) {
                ArticleSectionHeading(number, it)
            } else {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        if (body.isNotBlank()) {
            ArticleBody(body)
        }
    }
}
