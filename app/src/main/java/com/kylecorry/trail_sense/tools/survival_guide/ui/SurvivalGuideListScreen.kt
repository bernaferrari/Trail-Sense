@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult

data class SurvivalGuideListItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val tag: String?,
    val onClick: () -> Unit
)

/**
 * Survival guide: a search bar, the best answer to the search as a card when there is a confident one, then the
 * chapters (or the matching sections) as one group of rows.
 */
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
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    TrailSensePage(
        title = stringResource(R.string.survival_guide),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        item(key = "search") {
            TrailSenseSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search),
                clearContentDescription = stringResource(R.string.survival_guide_clear_search),
                modifier = Modifier.testTag("search")
            )
        }

        if (summary.isNotBlank() && firstResult != null) {
            item(key = "summary") {
                SurvivalGuideSummary(
                    summary = summary,
                    result = firstResult,
                    markdown = markdown,
                    onSummaryClick = onSummaryClick,
                    modifier = Modifier.animateItem()
                )
            }
        }

        // The chapters load in the background; until then nothing is shown rather than an empty list.
        if (listItems.isNotEmpty()) {
            if (query.isBlank()) {
                // Browsing: the first chapter (the overview) as the hero, the rest as a grid of shaped tiles.
                item(key = "list") {
                    Column(
                        modifier = Modifier.testTag("list"),
                        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                    ) {
                        SurvivalGuideHero(listItems.first(), chapters = listItems.size)
                        SurvivalGuideTileGrid(listItems.drop(1))
                    }
                }
            } else {
                item(key = "list") {
                    TrailSenseRowGroup(modifier = Modifier.testTag("list")) {
                        listItems.forEachIndexed { index, item ->
                            row { position -> SurvivalGuideRow(item, position, index) }
                        }
                    }
                }
            }
        }
    }
}

/** One hue and one shape per chapter, so each topic is recognisable at a glance. */
private val chapterAccents = listOf(
    TrailSenseAccents.Coral,
    TrailSenseAccents.Sky,
    TrailSenseAccents.Leaf,
    TrailSenseAccents.Sun,
    TrailSenseAccents.Lavender,
    TrailSenseAccents.Seafoam,
    TrailSenseAccents.Apricot,
    TrailSenseAccents.Rose,
    TrailSenseAccents.Periwinkle,
    TrailSenseAccents.Mint
)

@Composable
private fun chapterShape(index: Int): androidx.compose.ui.graphics.Shape = when (index % 9) {
    0 -> MaterialShapes.Cookie9Sided
    1 -> MaterialShapes.Sunny
    2 -> MaterialShapes.Clover4Leaf
    3 -> MaterialShapes.Pentagon
    4 -> MaterialShapes.Flower
    5 -> MaterialShapes.Cookie6Sided
    6 -> MaterialShapes.Gem
    7 -> MaterialShapes.Puffy
    else -> MaterialShapes.SoftBurst
}.toShape()

@Composable
private fun SurvivalGuideHero(item: SurvivalGuideListItem, chapters: Int) {
    SurvivalGuidePressableSurface(
        onClick = item.onClick,
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.testTag("guide_item")
    ) {
        Column(
            Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkyFocalShape(
                    icon = item.icon,
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    size = 84.dp,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.sky_survival_chapters, chapters),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("guide_item_title")
                    )
                }
                Icon(TrailIcons.KeyboardArrowRight, contentDescription = null)
            }
            item.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f)
                )
            }
        }
    }
}

/** Chapters as connected tiles, two to a row: a shaped icon badge in the chapter's hue, its name and one line. */
@Composable
private fun SurvivalGuideTileGrid(allItems: List<SurvivalGuideListItem>) {
    if (allItems.isEmpty()) return
    // First aid is the chapter you may need in a hurry: it gets a full-width tile in the error container.
    val medical = allItems.firstOrNull { it.icon == TrailIcons.CategoryMedical }
    val items = allItems - listOfNotNull(medical).toSet()
    val rows = items.chunked(2)
    // The caller's column spaces the feature tile and the grid apart.
    if (medical != null) {
        SurvivalGuidePressableSurface(
            onClick = medical.onClick,
            shape = RoundedCornerShape(TrailSenseShape.groupOuter),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.testTag("guide_item")
        ) {
            Row(Modifier.padding(TrailSenseSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
                TrailSenseShapeBadge(
                    icon = medical.icon,
                    shape = MaterialShapes.SoftBurst.toShape(),
                    size = 60.dp,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        medical.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("guide_item_title")
                    )
                    medical.subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SurvivalChevron(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                row.forEachIndexed { columnIndex, item ->
                    val index = rowIndex * 2 + columnIndex
                    val wells = trailSenseWellColors(chapterAccents[index % chapterAccents.size])
                    SurvivalGuidePressableSurface(
                        onClick = item.onClick,
                        shape = trailSenseGridCellShape(columnIndex, row.size, rowIndex, rows.size),
                        color = androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            wells.container,
                            0.4f
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("guide_item")
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 148.dp)
                                .padding(TrailSenseSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                TrailSenseShapeBadge(
                                    icon = item.icon,
                                    shape = chapterShape(index),
                                    size = 52.dp,
                                    containerColor = wells.content,
                                    contentColor = wells.container
                                )
                                Spacer(Modifier.weight(1f))
                                SurvivalChevron(
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
                                    wells.content
                                )
                            }
                            Spacer(Modifier.height(TrailSenseSpacing.sm))
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("guide_item_title")
                            )
                            item.subtitle?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurvivalGuideSummary(
    summary: String,
    result: SurvivalGuideSearchResult,
    markdown: MarkdownService,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = getSummaryTitle(result)
    SurvivalGuidePressableSurface(
        onClick = { onSummaryClick(result) },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.testTag("summary_holder")
    ) {
        Column(
            modifier = Modifier.padding(
                start = TrailSenseSpacing.xl,
                end = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.lg,
                bottom = TrailSenseSpacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrailSenseShapeBadge(
                    icon = TrailIcons.Search,
                    shape = MaterialShapes.Sunny.toShape(),
                    size = 48.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    Text(
                        text = result.chapter.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.testTag("summary_chapter_title")
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("summary_title")
                    )
                }
                Box(
                    modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                        contentDescription = null
                    )
                }
            }
            // The summary can run long: it scrolls inside the card so the chapters stay close below it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
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
}

@Composable
private fun SurvivalGuideRow(item: SurvivalGuideListItem, position: TrailSenseGroupPosition, index: Int) {
    // In search results the chapter leads the line so a heading like "Overview" still says where it lives.
    val subtitle = listOfNotNull(item.tag, item.subtitle?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }
    SurvivalGuideGroupRow(
        title = item.title,
        subtitle = subtitle,
        icon = rememberVectorPainter(item.icon),
        iconShape = chapterShape(index),
        accent = chapterAccents[index % chapterAccents.size],
        position = position,
        onClick = item.onClick,
        modifier = Modifier.testTag("guide_item"),
        titleModifier = Modifier.testTag("guide_item_title"),
        trailing = {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun getSummaryTitle(result: SurvivalGuideSearchResult): String {
    return listOfNotNull(
        result.heading,
        if (SurvivalGuideSearch.shouldUseSubsection(result)) {
            result.bestSubsection?.heading
        } else {
            null
        }
    ).joinToString(" › ")
}

/** A small circular arrow: the tile opens a chapter. */
@Composable
private fun SurvivalChevron(container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(TrailIcons.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
