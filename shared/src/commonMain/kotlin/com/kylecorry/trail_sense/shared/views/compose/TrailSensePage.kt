@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons

/** Goes back from the current screen, when there is somewhere to go back to (null on a tab's first screen). */
val LocalTrailSenseBack = androidx.compose.runtime.staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * A screen's title bar for screens that don't scroll as a page (instruments, maps, lists with their own scrolling):
 * a back arrow when there is somewhere to go back to, the title, and actions.
 */
@Composable
fun TrailSenseTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = LocalTrailSenseBack.current,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {}
) {
    androidx.compose.material3.TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(TrailSenseIcons.Filled.ArrowBack, contentDescription = backLabel)
                }
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

/**
 * The standard scrolling page of the family: a large title that collapses into the bar as the page scrolls, a back
 * button when there is somewhere to go back to, 16 dp gutters, content centered at a readable width on tablets, and
 * 16 dp between sections. Sections are usually [trailSenseSection]s.
 */
@Composable
fun TrailSensePage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = LocalTrailSenseBack.current,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(
        start = TrailSenseSpacing.lg,
        end = TrailSenseSpacing.lg,
        top = TrailSenseSpacing.sm,
        bottom = TrailSenseSpacing.xxxl
    ),
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = floatingActionButton,
        topBar = {
            val titleContent: @Composable () -> Unit = {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }
            val subtitleContent: (@Composable () -> Unit)? = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
            val navigation: @Composable () -> Unit = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(TrailSenseIcons.Filled.ArrowBack, contentDescription = backLabel)
                    }
                }
            }
            val barColors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
            // Like QuietGuard: a tab's own screen gets the large title; a screen opened from another one (every
            // tool) gets the medium bar, so the content starts higher and the title doesn't shout.
            if (onBack == null) {
                LargeFlexibleTopAppBar(
                    title = titleContent,
                    subtitle = subtitleContent,
                    navigationIcon = navigation,
                    actions = actions,
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = barColors,
                    scrollBehavior = scrollBehavior
                )
            } else {
                MediumFlexibleTopAppBar(
                    title = titleContent,
                    subtitle = subtitleContent,
                    navigationIcon = navigation,
                    actions = actions,
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = barColors,
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = TrailSenseSize.maximumContentWidth),
                // Screens opened from another one have no bottom bar, so the last row clears the gesture bar here.
                contentPadding = if (onBack != null) {
                    val navigationBar = androidx.compose.foundation.layout.WindowInsets.navigationBars
                        .asPaddingValues().calculateBottomPadding()
                    val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
                    PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(direction),
                        bottom = contentPadding.calculateBottomPadding() + navigationBar
                    )
                } else {
                    contentPadding
                },
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg),
                content = content
            )
        }
    }
}

/**
 * One section: an optional header and its rows, kept in one lazy item so the header never scrolls away from its group.
 * Rows are added with [TrailSenseRowGroupScope.row] and get their place in the group (round outside, tight where they
 * meet) from the rows actually shown, so a row hidden by a condition never leaves a square corner behind.
 */
fun LazyListScope.trailSenseSection(
    key: String,
    title: String? = null,
    footer: String? = null,
    rows: TrailSenseRowGroupScope.() -> Unit
) {
    item(key = key) {
        TrailSenseRowGroup(title = title, footer = footer, rows = rows)
    }
}

class TrailSenseRowGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable (TrailSenseGroupPosition) -> Unit>()

    fun row(content: @Composable (position: TrailSenseGroupPosition) -> Unit) {
        rows += content
    }
}

/** A titled group of rows outside a lazy list (or inside one lazy item). See [trailSenseSection]. */
@Composable
fun TrailSenseRowGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    rows: TrailSenseRowGroupScope.() -> Unit
) {
    val scope = TrailSenseRowGroupScope().apply(rows)
    if (scope.rows.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            TrailSenseGroupHeader(title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            scope.rows.forEachIndexed { index, row ->
                row(TrailSenseGroupPosition.at(index, scope.rows.size))
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm
                )
            )
        }
    }
}

/** The family's section header: primary title-small, owning the 8 dp between it and its group. */
@Composable
fun TrailSenseGroupHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.md,
            bottom = TrailSenseSpacing.sm
        )
    )
}

/** One option of a [TrailSenseConnectedChoice]. */
data class TrailSenseChoice<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * The family's one selection control, used for every "pick one of a few" choice (tabs inside a screen, units,
 * directions, modes): connected buttons 40 dp tall, the chosen one in secondaryContainer with a check. The label never
 * moves: every button keeps a leading slot (the option's icon, or the check when chosen), so choosing only swaps what
 * is in the slot. Buttons share the width equally; with [scrollable] (more options than fit, like Convert's six) they
 * take their natural width in a row that scrolls sideways. Read out as a radio group.
 */
@Composable
fun <T> TrailSenseConnectedChoice(
    options: List<TrailSenseChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    scrollable: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (scrollable) {
                    Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                } else {
                    Modifier
                }
            )
            .selectableGroup()
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val checked = option.value == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { if (it && !checked) onSelect(option.value) },
                modifier = Modifier
                    .then(if (scrollable) Modifier else Modifier.weight(1f))
                    .heightIn(min = TrailSenseChoiceDefaults.height)
                    .semantics { role = Role.RadioButton },
                shapes = when {
                    options.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = trailSenseChoiceColors(),
                contentPadding = PaddingValues(horizontal = TrailSenseSpacing.sm)
            ) {
                TrailSenseChoiceContent(label = option.label, icon = option.icon, checked = checked)
            }
        }
    }
}

object TrailSenseChoiceDefaults {
    /** Every selection control in the family is this tall (the touch target still grows to 48 dp). */
    val height = 40.dp
}

/** The selection colours: chosen = secondaryContainer, the rest a quiet raised surface. */
@Composable
fun trailSenseChoiceColors() = ToggleButtonDefaults.toggleButtonColors(
    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * A choice button's content with a fixed label position: the leading slot (icon, or check when chosen) is always
 * there, so the label sits in the same place whether or not it is chosen.
 */
@Composable
fun RowScope.TrailSenseChoiceContent(label: String, icon: ImageVector?, checked: Boolean) {
    val slot = ToggleButtonDefaults.IconSize
    val shown = if (checked) Icons.Rounded.Check else icon
    Box(Modifier.size(slot), contentAlignment = Alignment.Center) {
        if (shown != null) {
            Icon(shown, contentDescription = null, modifier = Modifier.size(slot))
        }
    }
    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
    Text(
        label,
        modifier = Modifier.padding(end = TrailSenseSpacing.xs),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** A group card holding anything (a chart, a choice, a preview) with the group's shape and tone. */
@Composable
fun TrailSenseGroupCard(
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(TrailSenseSpacing.lg),
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = color
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
