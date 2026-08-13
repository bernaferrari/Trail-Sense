@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TextBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult

data class SurvivalGuideListItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val icon: Int,
    val tag: String?,
    val onClick: () -> Unit
)

@Composable
fun SurvivalGuideListScreen(
    query: String,
    listItems: List<SurvivalGuideListItem>,
    summary: String,
    firstResult: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onQueryChange: (String) -> Unit,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .padding(horizontal = TrailSenseSpacing.screen)
        ) {
            SurvivalGuideSearchBox(
                query = query,
                onQueryChange = onQueryChange
            )
            if (summary.isNotBlank()) {
                SurvivalGuideSummary(
                    summary = summary,
                    result = firstResult,
                    markdown = markdown,
                    onSummaryClick = onSummaryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(top = TrailSenseSpacing.sm)
                )
            }
            SurvivalGuideChapterList(
                listItems = listItems,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (summary.isBlank()) TrailSenseSpacing.lg else TrailSenseSpacing.sm)
            )
        }
        }
    }
}

@Composable
private fun SurvivalGuideSearchBox(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(text = stringResource(R.string.search)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.lg)
            .testTag("search")
    )
}

@Composable
private fun SurvivalGuideSummary(
    summary: String,
    result: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) {
        return
    }

    TrailSenseCard(
        modifier = modifier.testTag("summary_holder"),
        onClick = { onSummaryClick(result) }
    ) {
        TextBadge(
            text = result.chapter.title,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            textStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.testTag("summary_chapter_title")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.sm, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getSummaryTitle(result),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_title"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = getSummaryTitle(result),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag("summary_scroll")
        ) {
            TrailSenseLinkedText(
                text = markdown.toMarkdown(summary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary"),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SurvivalGuideChapterList(
    listItems: List<SurvivalGuideListItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("list"),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        items(listItems, key = { it.id }) { item ->
            SurvivalGuideChapterRow(item)
        }
    }
}

@Composable
private fun SurvivalGuideChapterRow(item: SurvivalGuideListItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag("guide_item")
            .clickable { item.onClick() }
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(TrailSenseSize.leadingIcon)
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                modifier = Modifier.testTag("guide_item_title"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.tag != null) {
                Text(
                    text = item.tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

private fun getSummaryTitle(result: SurvivalGuideSearchResult): String {
    return listOfNotNull(
        result.heading,
        if (SurvivalGuideSearch.shouldUseSubsection(result)) {
            result.bestSubsection?.heading
        } else {
            null
        }
    ).joinToString(" > ")
}
