@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.icons.trail.Grass
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideCleanupCommand
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuideToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val formatter = remember(appContext) { FieldGuideFormatService(UserPreferences(appContext)) }
    val tagNameMapper = remember(appContext) { FieldGuideTagNameMapper(appContext) }
    val files = remember(appContext) { FileSubsystem.getInstance(appContext) }
    val viewModel = viewModel {
        FieldGuideListViewModel(
            repo = FieldGuideRepo.getInstance(appContext),
            cleanup = { FieldGuideCleanupCommand(appContext).execute() },
            sortName = { formatter.formatName(it).lowercase() },
            tagName = tagNameMapper::getName
        )
    }

    LaunchedEffect(Unit) {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.disclaimer_message_title),
            context.getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.reloadPages()
        onPauseOrDispose { }
    }

    // Back clears a search or category first, then leaves the field guide.
    BackHandler(enabled = viewModel.isFiltering) {
        viewModel.clearFilters()
    }

    val filteredPages = viewModel.filteredPages
    val items = if (!viewModel.isFiltering) {
        FieldGuideListItems(
            FIELD_GUIDE_HEADING_TAGS.map { tag ->
                val count = filteredPages.count { it.tags.contains(tag) }
                FieldGuideListItem.TagItem(
                    tag = tag,
                    title = tagNameMapper.getName(tag),
                    subtitle = context.resources.getQuantityString(
                        R.plurals.page_group_summary,
                        count,
                        count
                    ),
                    icon = fieldGuideTagIcon(tag),
                    count = count,
                    onClick = { viewModel.tagFilter = tag }
                )
            },
            totalPages = filteredPages.size
        )
    } else {
        FieldGuideListItems(
            filteredPages.map {
                FieldGuideListItem.PageItem(
                    page = it,
                    title = formatter.formatNameAnnotated(it),
                    subtitle = it.notes?.substringBefore(".")
                        ?.plus(if (it.notes.contains(".")) "." else "")
                        ?.take(200)
                        ?: ""
                )
            }
        )
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    FieldGuideScreen(
        query = viewModel.filter,
        loaded = viewModel.loaded,
        items = items,
        tagFilter = viewModel.tagFilter,
        tagFilters = FIELD_GUIDE_HEADING_TAGS.map { it to tagNameMapper.getName(it) },
        tagIcon = ::fieldGuideTagIcon,
        onTagSelected = viewModel::toggleTag,
        onBack = { dispatcher?.onBackPressed() },
        onQueryChange = { viewModel.filter = it },
        onViewPage = { navigator.navigate(Route.FieldGuidePage(pageId = it.id)) },
        onEditPage = { navigator.navigate(Route.CreateFieldGuidePage(pageId = it.id)) },
        onDeletePage = { page ->
            Alerts.dialog(
                context,
                context.getString(R.string.delete),
                formatter.formatName(page)
            ) { cancelled ->
                if (!cancelled) {
                    viewModel.deletePage(page)
                }
            }
        },
        onHidePage = { page ->
            Alerts.dialog(
                context,
                context.getString(R.string.hide),
                context.getString(R.string.hide_field_guide_page_message, formatter.formatName(page))
            ) { cancelled ->
                if (!cancelled) {
                    viewModel.hidePage(page)
                }
            }
        },
        loadThumbnail = { page ->
            onIO {
                val path = page.images.firstOrNull() ?: return@onIO null
                val size = Resources.dp(appContext, 48f).toInt()
                try {
                    files.bitmap(path, Size(size, size))
                } catch (e: Exception) {
                    null
                }
            }
        },
        onCreate = {
            navigator.navigate(Route.CreateFieldGuidePage(classificationId = viewModel.tagFilter?.id ?: 0L))
        }
    )
}

/**
 * The field guide's pages (cleaned up and reread each time the list is shown), the search text and the category
 * being browsed, and which pages they leave.
 */
internal class FieldGuideListViewModel(
    private val repo: FieldGuideRepo,
    private val cleanup: suspend () -> Unit,
    private val sortName: (FieldGuidePage) -> String,
    private val tagName: (FieldGuidePageTag) -> String
) : ViewModel() {

    var pages by mutableStateOf(emptyList<FieldGuidePage>())
        private set
    var loaded by mutableStateOf(false)
        private set
    var filter by mutableStateOf("")
    var tagFilter by mutableStateOf<FieldGuidePageTag?>(null)

    /** True while a search or a category narrows the list. */
    val isFiltering: Boolean
        get() = tagFilter != null || filter.isNotBlank()

    /** The visible pages that match the search text and category. */
    val filteredPages: List<FieldGuidePage>
        get() = TextUtils.search(filter, pages.filter { !it.isHidden }) { page ->
            listOf(
                page.name,
                page.scientificName ?: "",
                page.notes ?: "",
                page.tags.joinToString { tagName(it) }
            )
        }.filter {
            tagFilter == null || it.tags.contains(tagFilter)
        }

    fun clearFilters() {
        tagFilter = null
        filter = ""
    }

    fun toggleTag(tag: FieldGuidePageTag) {
        tagFilter = if (tagFilter == tag) null else tag
    }

    fun reloadPages() {
        viewModelScope.launch {
            pages = withContext(Dispatchers.IO) {
                cleanup()
                repo.getAllPages().sortedBy(sortName)
            }
            loaded = true
        }
    }

    fun hidePage(page: FieldGuidePage) {
        viewModelScope.launch {
            val hidden = withContext(Dispatchers.IO) {
                repo.setPageHidden(page.id, true)
            }
            if (hidden) {
                reloadPages()
            }
        }
    }

    fun deletePage(page: FieldGuidePage) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.delete(page)
            }
            reloadPages()
        }
    }
}

private val FIELD_GUIDE_HEADING_TAGS = listOf(
    FieldGuidePageTag.Plant,
    FieldGuidePageTag.Fungus,
    FieldGuidePageTag.Animal,
    FieldGuidePageTag.Mammal,
    FieldGuidePageTag.Bird,
    FieldGuidePageTag.Reptile,
    FieldGuidePageTag.Amphibian,
    FieldGuidePageTag.Fish,
    FieldGuidePageTag.Invertebrate,
    FieldGuidePageTag.Rock,
    FieldGuidePageTag.Weather,
    FieldGuidePageTag.Other
)

private fun fieldGuideTagIcon(tag: FieldGuidePageTag): ImageVector = when (tag) {
    FieldGuidePageTag.Plant -> TrailIcons.CategoryNatural
    FieldGuidePageTag.Fungus -> TrailIcons.Mushroom
    FieldGuidePageTag.Animal -> TrailIcons.Paw
    FieldGuidePageTag.Mammal -> TrailIcons.Deer
    FieldGuidePageTag.Bird -> TrailIcons.Bird
    FieldGuidePageTag.Reptile -> TrailIcons.Lizard
    FieldGuidePageTag.Amphibian -> TrailIcons.Frog
    FieldGuidePageTag.Fish -> TrailIcons.Fish
    FieldGuidePageTag.Invertebrate -> TrailIcons.Ant
    FieldGuidePageTag.Rock -> TrailIcons.Gem
    FieldGuidePageTag.Weather -> TrailIcons.Cloud
    else -> TrailIcons.Help
}

@Composable
private fun FieldGuideScreen(
    query: String,
    loaded: Boolean,
    items: FieldGuideListItems,
    tagFilter: FieldGuidePageTag?,
    tagFilters: List<Pair<FieldGuidePageTag, String>>,
    tagIcon: (FieldGuidePageTag) -> ImageVector,
    onTagSelected: (FieldGuidePageTag) -> Unit,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onViewPage: (FieldGuidePage) -> Unit,
    onEditPage: (FieldGuidePage) -> Unit,
    onDeletePage: (FieldGuidePage) -> Unit,
    onHidePage: (FieldGuidePage) -> Unit,
    loadThumbnail: suspend (FieldGuidePage) -> Bitmap?,
    onCreate: () -> Unit
) {
    val isBrowsing = tagFilter == null && query.isBlank()
    val selectedTagName = tagFilters.firstOrNull { it.first == tagFilter }?.second
    val pages = items.items.filterIsInstance<FieldGuideListItem.PageItem>()
    val categories = items.items.filterIsInstance<FieldGuideListItem.TagItem>()
    val sections = remember(pages) { groupPagesByLetter(pages) }
    val noPagesDescription = stringResource(R.string.field_guide_no_pages_description)

    // While there is more list under it, the button folds to its icon so it covers only the rows' empty trailing
    // edge; at the end of the list (which leaves room for it) it shows its label again.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val addExpanded by remember {
        androidx.compose.runtime.derivedStateOf { !listState.canScrollForward }
    }

    // One question: what is this plant or animal? Search and the kinds answer it; the page count is the one figure.
    TrailSensePage(
        state = listState,
        title = stringResource(R.string.field_guide),
        subtitle = selectedTagName,
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = Modifier.testTag("list"),
        // Room under the last row for the floating button.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.sm,
            bottom = 112.dp
        ),
        floatingActionButton = {
            // The screen's one main action. An empty result has its own button, so nothing floats over it.
            if (!(loaded && !isBrowsing && pages.isEmpty())) ExtendedFloatingActionButton(
                onClick = onCreate,
                expanded = addExpanded,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_btn"),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.add)) }
            )
        }
    ) {
        if (isBrowsing && loaded) {
            item(key = "notebook") {
                FieldGuideNotebookHero(
                    pages = items.totalPages,
                    kinds = categories.count { it.count > 0 }
                )
            }
        }

        item(key = "search") {
            TrailSenseSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search),
                clearContentDescription = stringResource(R.string.field_guide_clear_search),
                modifier = Modifier.testTag("search")
            )
        }

        // Once a category or a search narrows the list, the categories become filter chips at the top so the
        // reader can hop between them without going back.
        if (!isBrowsing) {
            item(key = "tag_filters") {
                FieldGuideTagFilters(
                    tags = tagFilters,
                    selected = tagFilter,
                    tagIcon = tagIcon,
                    onSelect = onTagSelected
                )
            }
        }

        if (!loaded) {
            return@TrailSensePage
        }

        if (isBrowsing) {
            item(key = "categories") {
                FieldGuideCategoryGrid(categories)
            }
        } else if (pages.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = stringResource(R.string.no_pages),
                    description = noPagesDescription,
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("empty_text"),
                    actionLabel = stringResource(R.string.create_page),
                    actionIcon = Icons.Rounded.Add,
                    onAction = onCreate
                )
            }
        } else {
            sections.forEach { section ->
                trailSenseSection(key = "letter_${section.title}", title = section.title) {
                    section.pages.forEach { item ->
                        row { position ->
                            FieldGuidePageRow(
                                item = item,
                                position = position,
                                onViewPage = onViewPage,
                                onEditPage = onEditPage,
                                onDeletePage = onDeletePage,
                                onHidePage = onHidePage,
                                loadThumbnail = loadThumbnail
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class FieldGuidePageSection(
    val title: String?,
    val pages: List<FieldGuideListItem.PageItem>
)

/**
 * Short lists read best as one group. Longer ones get an index of first letters so the reader can find their place
 * while scrolling; the pages already arrive sorted by name.
 */
private fun groupPagesByLetter(pages: List<FieldGuideListItem.PageItem>): List<FieldGuidePageSection> {
    if (pages.size <= LETTER_SECTION_THRESHOLD) {
        return listOf(FieldGuidePageSection(null, pages))
    }
    return pages
        .groupBy { page ->
            val first = page.title.text.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (first.isLetter()) first.toString() else "#"
        }
        .map { (letter, items) -> FieldGuidePageSection(letter, items) }
}

private const val LETTER_SECTION_THRESHOLD = 12

@Composable
private fun FieldGuideTagFilters(
    tags: List<Pair<FieldGuidePageTag, String>>,
    selected: FieldGuidePageTag?,
    tagIcon: (FieldGuidePageTag) -> ImageVector,
    onSelect: (FieldGuidePageTag) -> Unit
) {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = tags.indexOfFirst { it.first == selected }
        if (index > 0) {
            state.scrollToItem(index)
        }
    }
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_guide_tag_filters"),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        items(tags, key = { it.first.id }) { (tag, name) ->
            val isSelected = tag == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(tag) },
                label = { Text(name) },
                leadingIcon = {
                    Icon(
                        painter = if (isSelected) {
                            rememberVectorPainter(Icons.Rounded.Check)
                        } else {
                            rememberVectorPainter(tagIcon(tag))
                        },
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
            )
        }
    }
}

@Composable
private fun FieldGuidePageRow(
    item: FieldGuideListItem.PageItem,
    position: TrailSenseGroupPosition,
    onViewPage: (FieldGuidePage) -> Unit,
    onEditPage: (FieldGuidePage) -> Unit,
    onDeletePage: (FieldGuidePage) -> Unit,
    onHidePage: (FieldGuidePage) -> Unit,
    loadThumbnail: suspend (FieldGuidePage) -> Bitmap?
) {
    var menuOpen by remember { mutableStateOf(false) }
    var thumbnail by remember(item.page.id, item.page.images) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(item.page.id, item.page.images) {
        thumbnail = loadThumbnail(item.page)
    }

    FieldGuideRow(
        title = item.title,
        subtitle = item.subtitle,
        position = position,
        titleModifier = Modifier.testTag("field_guide_page_title"),
        leading = {
            FieldGuideThumbnail(
                bitmap = thumbnail,
                fallback = rememberVectorPainter(Icons.AutoMirrored.Rounded.MenuBook)
            )
        },
        onClick = { onViewPage(item.page) },
        onLongClick = { menuOpen = true },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("field_guide_page_menu")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.field_guide_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    if (item.page.isReadOnly) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hide)) },
                            onClick = {
                                menuOpen = false
                                onHidePage(item.page)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuOpen = false
                                onEditPage(item.page)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                menuOpen = false
                                onDeletePage(item.page)
                            }
                        )
                    }
                }
            }
        }
    )
}

private data class FieldGuideListItems(
    val items: List<FieldGuideListItem>,
    val totalPages: Int = 0
)

private sealed interface FieldGuideListItem {
    val id: Long

    data class TagItem(
        val tag: FieldGuidePageTag,
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val count: Int = 0,
        val onClick: () -> Unit
    ) : FieldGuideListItem {
        override val id: Long = tag.id
    }

    data class PageItem(
        val page: FieldGuidePage,
        val title: AnnotatedString,
        val subtitle: String
    ) : FieldGuideListItem {
        override val id: Long = page.id
    }
}

/** The notebook on the plain page: how many pages it holds in emphasised type, and across how many kinds. */
@Composable
private fun FieldGuideNotebookHero(
    pages: Int,
    kinds: Int
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg)
    ) {
        Text(
            androidx.compose.ui.res.pluralStringResource(R.plurals.page_group_summary, pages, pages),
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.sky_field_guide_kinds, kinds),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = TrailSenseSpacing.xxs)
        )
    }
}

/**
 * The kinds as one grouped list: the kind's icon in the screen's one shape, its name, and how many pages it holds
 * as the supporting line ("No pages" when empty), so an empty kind reads as empty, not as a missing number.
 */
@Composable
private fun FieldGuideCategoryGrid(categories: List<FieldGuideListItem.TagItem>) {
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup {
        categories.forEach { category ->
            row { position ->
                SkyValueRow(
                    title = category.title,
                    subtitle = if (category.count > 0) {
                        androidx.compose.ui.res.pluralStringResource(
                            R.plurals.page_group_summary,
                            category.count,
                            category.count
                        )
                    } else {
                        stringResource(R.string.no_pages)
                    },
                    icon = category.icon,
                    position = position,
                    onClick = category.onClick
                )
            }
        }
    }
}
