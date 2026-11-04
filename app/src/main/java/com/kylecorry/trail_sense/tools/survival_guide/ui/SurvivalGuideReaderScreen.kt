@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.survival_guide.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.TextUtils.TextSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter

/**
 * One survival guide chapter: the chapter's introduction as plain reading text, then its sections as one group of
 * rows that open in place. Opening the reader at a search result scrolls to and opens that section.
 */
@Composable
fun SurvivalGuideReaderScreen(
    chapter: Chapter,
    content: String?,
    headerIndex: Int,
    canPrint: Boolean,
    onPrint: (Chapter) -> Unit
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val sectionGroups = remember(content) {
        content?.let { TextUtils.groupSections(TextUtils.getSections(it), null) }
    }
    val blocks = remember(sectionGroups) { sectionGroups?.let(::toBlocks) }
    val listState = rememberLazyListState()
    val expandedSections = remember(sectionGroups, headerIndex) { mutableStateMapOf<Int, Boolean>() }
    val targetGroupIndex = remember(sectionGroups, headerIndex) {
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
    // Where the target section sits inside its group, measured once it's laid out.
    var targetOffset by remember(targetGroupIndex) { mutableStateOf<Int?>(null) }

    LaunchedEffect(targetGroupIndex) {
        val target = targetGroupIndex ?: return@LaunchedEffect
        expandedSections[target] = true
    }

    LaunchedEffect(targetGroupIndex, targetOffset) {
        val target = targetGroupIndex ?: return@LaunchedEffect
        val offset = targetOffset ?: return@LaunchedEffect
        val blockIndex = blocks?.indexOfFirst { block ->
            block is ReaderBlock.Sections && block.groups.any { it.index == target }
        } ?: return@LaunchedEffect
        if (blockIndex >= 0) {
            listState.scrollToItem(blockIndex, offset)
        }
    }

    TrailSensePage(
        title = chapter.title,
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        state = listState,
        modifier = Modifier.testTag("guide_scroll"),
        actions = {
            if (canPrint) {
                IconButton(onClick = { onPrint(chapter) }) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Printer),
                        contentDescription = stringResource(R.string.print)
                    )
                }
            }
        },
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.sm,
            bottom = TrailSenseSpacing.xxxl
        )
    ) {
        if (blocks == null) {
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
            return@TrailSensePage
        }

        blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is ReaderBlock.Text -> item(key = "text_$blockIndex") {
                    SelectionContainer {
                        MarkdownText(
                            text = block.markdown,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.xs),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is ReaderBlock.Sections -> item(key = "sections_$blockIndex") {
                    TrailSenseRowGroup {
                        block.groups.forEach { (index, sections) ->
                            row { position ->
                                ReaderSection(
                                    sections = sections,
                                    position = position,
                                    expanded = expandedSections[index] ?: false,
                                    onExpandedChange = { expandedSections[index] = it },
                                    modifier = if (index == targetGroupIndex) {
                                        Modifier.onGloballyPositioned {
                                            if (targetOffset == null) {
                                                targetOffset = it.positionInParent().y.toInt()
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
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
private fun ReaderSection(
    sections: List<TextSection>,
    position: TrailSenseGroupPosition,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = sections.firstOrNull()?.title ?: return
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "sectionChevron"
    )
    val expandedLabel = stringResource(if (expanded) R.string.survival_guide_section_open else R.string.survival_guide_section_closed)
    TrailSenseGroupCard(
        position = position,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                .semantics { stateDescription = expandedLabel }
                .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            SelectionContainer {
                MarkdownText(
                    text = buildSectionMarkdown(sections),
                    modifier = Modifier.padding(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.lg
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
        DependencyRegistry.get<MarkdownService>().toMarkdown(text)
    }
    TrailSenseLinkedText(
        text = parsed,
        modifier = modifier.fillMaxWidth(),
        style = style
    )
}

/** Untitled text reads as prose; runs of titled sections become one group of rows. */
private sealed interface ReaderBlock {
    data class Text(val markdown: String) : ReaderBlock
    data class Sections(val groups: List<IndexedValue<List<TextSection>>>) : ReaderBlock
}

private fun toBlocks(groups: List<List<TextSection>>): List<ReaderBlock> {
    val blocks = mutableListOf<ReaderBlock>()
    var run = mutableListOf<IndexedValue<List<TextSection>>>()
    groups.withIndex().forEach { group ->
        if (group.value.firstOrNull()?.title == null) {
            if (run.isNotEmpty()) {
                blocks += ReaderBlock.Sections(run)
                run = mutableListOf()
            }
            blocks += ReaderBlock.Text(group.value.joinToString("\n") { it.toMarkdown() })
        } else {
            run += group
        }
    }
    if (run.isNotEmpty()) {
        blocks += ReaderBlock.Sections(run)
    }
    return blocks
}

private fun buildSectionMarkdown(sections: List<TextSection>): String {
    val first = sections.firstOrNull() ?: return ""
    return TextUtils.removeMarkdownComments(first.content) + "\n" +
        sections.drop(1).joinToString("\n") {
            it.toMarkdown(shouldUppercaseTitle = true)
        }
}
