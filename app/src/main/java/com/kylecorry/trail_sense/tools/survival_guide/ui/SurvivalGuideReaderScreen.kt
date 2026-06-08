@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter

@Composable
fun SurvivalGuideReaderScreen(
    chapter: Chapter,
    content: String?,
    headerIndex: Int,
    canPrint: Boolean,
    onPrint: (Chapter) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurvivalGuideHeader(
                title = chapter.title,
                canPrint = canPrint,
                onPrint = { onPrint(chapter) }
            )
            MarkdownChapterView(
                content = content,
                headerIndex = headerIndex,
                modifier = Modifier
                    .weight(1f)
                    .testTag("guide_scroll")
            )
        }
    }
}

@Composable
private fun SurvivalGuideHeader(
    title: String,
    canPrint: Boolean,
    onPrint: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .testTag("guide_name"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canPrint) {
                IconButton(onClick = onPrint) {
                    Icon(
                        painter = painterResource(R.drawable.ic_printer),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownChapterView(
    content: String?,
    headerIndex: Int,
    modifier: Modifier = Modifier
) {
    val sectionGroups = remember(content) {
        content?.let {
            TextUtils.groupSections(TextUtils.getSections(it), null)
        }
    }
    val listState = rememberLazyListState()
    val expandedSections = remember(sectionGroups, headerIndex) {
        mutableStateMapOf<Int, Boolean>()
    }
    val targetHeaderIndex = remember(sectionGroups, headerIndex) {
        if (headerIndex <= 0) {
            null
        } else {
            sectionGroups
                ?.withIndex()
                ?.filter { it.value.firstOrNull()?.title != null }
                ?.elementAtOrNull(headerIndex - 1)
                ?.index
        }
    }

    LaunchedEffect(targetHeaderIndex) {
        val target = targetHeaderIndex ?: return@LaunchedEffect
        expandedSections[target] = true
        listState.scrollToItem(target)
    }

    SelectionContainer(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val groups = sectionGroups
            if (groups == null) {
                item {
                    Text(
                        text = stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(groups.size) { index ->
                    MarkdownSectionGroup(
                        sections = groups[index],
                        expanded = expandedSections[index] ?: false,
                        onExpandedChange = { expandedSections[index] = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownSectionGroup(
    sections: List<TextSection>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val first = sections.firstOrNull() ?: return
    val title = first.title
    if (title == null) {
        MarkdownText(
            text = sections.joinToString("\n") { it.toMarkdown() },
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    TrailSenseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            MarkdownText(
                text = buildSectionMarkdown(sections),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val parsed = remember(text) {
        AppServiceRegistry.get<MarkdownService>().toMarkdown(text)
    }
    TrailSenseLinkedText(
        text = parsed,
        modifier = modifier.fillMaxWidth(),
        style = style
    )
}

private fun buildSectionMarkdown(sections: List<TextSection>): String {
    val first = sections.firstOrNull() ?: return ""
    return TextUtils.removeMarkdownComments(first.content) + "\n" +
        sections.drop(1).joinToString("\n") {
            it.toMarkdown(shouldUppercaseTitle = true)
        }
}
