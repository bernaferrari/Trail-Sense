@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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

    FieldGuideScreen(
        query = controller.filter,
        items = controller.getListItems(),
        onQueryChange = { controller.filter = it },
        onViewPage = controller::viewPage,
        onEditPage = controller::editPage,
        onDeletePage = controller::deletePage,
        loadThumbnail = controller::loadThumbnail,
        onCreate = { controller.createPage(controller.tagFilter) }
    )
}

private class FieldGuideController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(host.requireContext()) }
    private var pages by mutableStateOf(emptyList<FieldGuidePage>())
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
        FieldGuidePageTag.Plant to R.drawable.ic_category_natural,
        FieldGuidePageTag.Fungus to R.drawable.mushroom,
        FieldGuidePageTag.Animal to R.drawable.paw,
        FieldGuidePageTag.Mammal to R.drawable.ic_deer,
        FieldGuidePageTag.Bird to R.drawable.bird,
        FieldGuidePageTag.Reptile to R.drawable.lizard,
        FieldGuidePageTag.Amphibian to R.drawable.frog,
        FieldGuidePageTag.Fish to R.drawable.fish,
        FieldGuidePageTag.Invertebrate to R.drawable.ant,
        FieldGuidePageTag.Rock to R.drawable.gem,
        FieldGuidePageTag.Weather to R.drawable.cloud,
        FieldGuidePageTag.Other to R.drawable.ic_help
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
                repo.getAllPages().sortedBy { it.name }
            }
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
                        icon = tagImageMap[tag] ?: R.drawable.ic_help,
                        onClick = { tagFilter = tag }
                    )
                }
            )
        } else {
            FieldGuideListItems(
                filteredPages.map {
                    FieldGuideListItem.PageItem(
                        page = it,
                        subtitle = it.notes?.substringBefore(".")
                            ?.plus(if (it.notes.contains(".")) "." else "")
                            ?.take(200)
                            ?: ""
                    )
                }
            )
        }
    }

    private fun getFilteredPages(): List<FieldGuidePage> {
        val mapper = FieldGuideTagNameMapper(host.requireContext())
        return TextUtils.search(filter, pages) { page ->
            listOf(
                page.name,
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

    fun deletePage(page: FieldGuidePage) {
        Alerts.dialog(host.requireContext(), host.getString(R.string.delete), page.name) { cancelled ->
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

    suspend fun loadThumbnail(page: FieldGuidePage): Bitmap = onIO {
        val size = Resources.dp(host.requireContext(), 48f).toInt()
        try {
            files.bitmap(page.images.first(), Size(size, size)) ?: getDefaultThumbnail(size)
        } catch (e: Exception) {
            getDefaultThumbnail(size)
        }
    }

    private fun getDefaultThumbnail(size: Int): Bitmap {
        return createBitmap(size, size)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldGuideScreen(
    query: String,
    items: FieldGuideListItems,
    onQueryChange: (String) -> Unit,
    onViewPage: (FieldGuidePage) -> Unit,
    onEditPage: (FieldGuidePage) -> Unit,
    onDeletePage: (FieldGuidePage) -> Unit,
    loadThumbnail: suspend (FieldGuidePage) -> Bitmap,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.field_guide),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    modifier = Modifier.testTag("add_btn"),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FieldGuideList(
                    query = query,
                    items = items,
                    onQueryChange = onQueryChange,
                    onViewPage = onViewPage,
                    onEditPage = onEditPage,
                    onDeletePage = onDeletePage,
                    loadThumbnail = loadThumbnail,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FieldGuideList(
    query: String,
    items: FieldGuideListItems,
    onQueryChange: (String) -> Unit,
    onViewPage: (FieldGuidePage) -> Unit,
    onEditPage: (FieldGuidePage) -> Unit,
    onDeletePage: (FieldGuidePage) -> Unit,
    loadThumbnail: suspend (FieldGuidePage) -> Bitmap,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("list"),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.lg,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        item(key = "search") {
            TrailSenseSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search),
                modifier = Modifier.testTag("search")
            )
        }

        if (items.items.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.no_pages),
                    modifier = Modifier.testTag("empty_text")
                )
            }
        } else {
            items(items.items, key = { it.id }) { item ->
                when (item) {
                    is FieldGuideListItem.TagItem -> {
                        FieldGuideTagRow(item = item)
                    }

                    is FieldGuideListItem.PageItem -> {
                        FieldGuidePageRow(
                            item = item,
                            onViewPage = onViewPage,
                            onEditPage = onEditPage,
                            onDeletePage = onDeletePage,
                            loadThumbnail = loadThumbnail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldGuideTagRow(item: FieldGuideListItem.TagItem) {
    TrailSenseCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = item.onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.lg)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePageRow(
    item: FieldGuideListItem.PageItem,
    onViewPage: (FieldGuidePage) -> Unit,
    onEditPage: (FieldGuidePage) -> Unit,
    onDeletePage: (FieldGuidePage) -> Unit,
    loadThumbnail: suspend (FieldGuidePage) -> Bitmap
) {
    var menuOpen by remember { mutableStateOf(false) }
    var thumbnail by remember(item.page.id, item.page.images) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(item.page.id, item.page.images) {
        thumbnail = loadThumbnail(item.page)
    }

    TrailSenseCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onViewPage(item.page) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.lg)
            ) {
                Text(
                    text = item.page.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("field_guide_page_title")
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!item.page.isReadOnly) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("field_guide_page_menu")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_dots),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
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
    }
}

private data class FieldGuideListItems(
    val items: List<FieldGuideListItem>
)

private sealed interface FieldGuideListItem {
    val id: Long

    data class TagItem(
        val tag: FieldGuidePageTag,
        val title: String,
        val subtitle: String,
        val icon: Int,
        val onClick: () -> Unit
    ) : FieldGuideListItem {
        override val id: Long = tag.id
    }

    data class PageItem(
        val page: FieldGuidePage,
        val subtitle: String
    ) : FieldGuideListItem {
        override val id: Long = page.id
    }
}
