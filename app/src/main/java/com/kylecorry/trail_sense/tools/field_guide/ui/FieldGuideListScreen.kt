@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

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
import android.os.Bundle
import android.util.Size
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
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
fun FieldGuideToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { FieldGuideController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.reloadPages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    FieldGuideScreen(
        query = controller.filter,
        loaded = controller.loaded,
        items = controller.getListItems(),
        tagFilter = controller.tagFilter,
        tagFilters = controller.getTagFilters(),
        tagIcon = controller::getTagIcon,
        onTagSelected = controller::toggleTag,
        onBack = { dispatcher?.onBackPressed() },
        onQueryChange = { controller.filter = it },
        onViewPage = controller::viewPage,
        onEditPage = controller::editPage,
        onDeletePage = controller::deletePage,
        onHidePage = controller::hidePage,
        loadThumbnail = controller::loadThumbnail,
        onCreate = { controller.createPage(controller.tagFilter) }
    )
}

private class FieldGuideController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(host.requireContext()) }
    private val formatter by lazy { FieldGuideFormatService(UserPreferences(host.requireContext())) }
    private var pages by mutableStateOf(emptyList<FieldGuidePage>())
    var loaded by mutableStateOf(false)
        private set
    var filter by mutableStateOf("")
    var tagFilter by mutableStateOf<FieldGuidePageTag?>(null)
    private var backCallback: OnBackPressedCallback? = null
    private var isInitialized = false

    private val headingTags = listOf(
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

    private val tagImageMap = mapOf(
        FieldGuidePageTag.Plant to TrailIcons.CategoryNatural,
        FieldGuidePageTag.Fungus to TrailIcons.Mushroom,
        FieldGuidePageTag.Animal to TrailIcons.Paw,
        FieldGuidePageTag.Mammal to TrailIcons.Deer,
        FieldGuidePageTag.Bird to TrailIcons.Bird,
        FieldGuidePageTag.Reptile to TrailIcons.Lizard,
        FieldGuidePageTag.Amphibian to TrailIcons.Frog,
        FieldGuidePageTag.Fish to TrailIcons.Fish,
        FieldGuidePageTag.Invertebrate to TrailIcons.Ant,
        FieldGuidePageTag.Rock to TrailIcons.Gem,
        FieldGuidePageTag.Weather to TrailIcons.Cloud,
        FieldGuidePageTag.Other to TrailIcons.Help
    )

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        CustomUiUtils.disclaimer(
            host.requireContext(),
            host.getString(R.string.disclaimer_message_title),
            host.getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tagFilter != null || filter.isNotBlank()) {
                    tagFilter = null
                    filter = ""
                } else {
                    isEnabled = false
                    host.mainActivity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        host.mainActivity.onBackPressedDispatcher.addCallback(
            host.lifecycleOwner,
            backCallback!!
        )
        reloadPages()
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
    }

    fun reloadPages() {
        host.lifecycleScope.launch {
            pages = withContext(Dispatchers.IO) {
                FieldGuideCleanupCommand(host.requireContext()).execute()
                repo.getAllPages().sortedBy { formatter.formatName(it).lowercase() }
            }
            loaded = true
        }
    }

    fun getListItems(): FieldGuideListItems {
        val filteredPages = getFilteredPages()
        val tagNameMapper = FieldGuideTagNameMapper(host.requireContext())
        return if (tagFilter == null && filter.isBlank()) {
            FieldGuideListItems(
                headingTags.map { tag ->
                    FieldGuideListItem.TagItem(
                        tag = tag,
                        title = tagNameMapper.getName(tag),
                        subtitle = host.getResources().getQuantityString(
                            R.plurals.page_group_summary,
                            filteredPages.count { it.tags.contains(tag) },
                            filteredPages.count { it.tags.contains(tag) }
                        ),
                        icon = tagImageMap[tag] ?: TrailIcons.Help,
                        count = filteredPages.count { it.tags.contains(tag) },
                        onClick = { tagFilter = tag }
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
    }

    fun getTagFilters(): List<Pair<FieldGuidePageTag, String>> {
        val mapper = FieldGuideTagNameMapper(host.requireContext())
        return headingTags.map { it to mapper.getName(it) }
    }

    fun getTagIcon(tag: FieldGuidePageTag): ImageVector {
        return tagImageMap[tag] ?: TrailIcons.Help
    }

    fun toggleTag(tag: FieldGuidePageTag) {
        tagFilter = if (tagFilter == tag) null else tag
    }

    private fun getFilteredPages(): List<FieldGuidePage> {
        val mapper = FieldGuideTagNameMapper(host.requireContext())
        return TextUtils.search(filter, pages.filter { !it.isHidden }) { page ->
            listOf(
                page.name,
                page.scientificName ?: "",
                page.notes ?: "",
                page.tags.joinToString { mapper.getName(it) }
            )
        }.filter {
            tagFilter == null || it.tags.contains(tagFilter)
        }
    }

    fun createPage(tag: FieldGuidePageTag?) {
        host.findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("classification_id", tag?.id ?: 0L)
            }
        )
    }

    fun editPage(page: FieldGuidePage) {
        host.findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", page.id)
            }
        )
    }

    fun viewPage(page: FieldGuidePage) {
        host.findNavController().navigate(
            R.id.fieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", page.id)
            }
        )
    }

    fun hidePage(page: FieldGuidePage) {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.hide),
            host.getString(R.string.hide_field_guide_page_message, formatter.formatName(page))
        ) { cancelled ->
            if (!cancelled) {
                host.lifecycleScope.launch {
                    val hidden = withContext(Dispatchers.IO) {
                        repo.setPageHidden(page.id, true)
                    }
                    if (hidden) {
                        reloadPages()
                    }
                }
            }
        }
    }

    fun deletePage(page: FieldGuidePage) {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete),
            formatter.formatName(page)
        ) { cancelled ->
            if (!cancelled) {
                host.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repo.delete(page)
                    }
                    reloadPages()
                }
            }
        }
    }

    suspend fun loadThumbnail(page: FieldGuidePage): Bitmap? = onIO {
        val path = page.images.firstOrNull() ?: return@onIO null
        val size = Resources.dp(host.requireContext(), 48f).toInt()
        try {
            files.bitmap(path, Size(size, size))
        } catch (e: Exception) {
            null
        }
    }
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

    // One question: what is this plant or animal? Search and the kinds answer it; the page count is the one figure.
    TrailSensePage(
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
                shape = CircleShape,
                border = null,
                // The family's choice colours: the chosen one solid primary, the rest a quiet raised surface.
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
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
