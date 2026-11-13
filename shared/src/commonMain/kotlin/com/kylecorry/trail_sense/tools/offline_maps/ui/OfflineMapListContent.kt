@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.offline_maps.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.appPluralString
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.OfflineMapsEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.ClosestMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MapSortMethod
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MostRecentMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.NameMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapPreferences
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ToggleVisibilityMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapGroupAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.VectorMapAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun setGroupVisibility(
    host: AppHost,
    scope: CoroutineScope,
    viewModel: OfflineMapListViewModel,
    group: MapGroup,
    visible: Boolean
) {
    scope.launch {
        val loading = Alerts.loading(host, getAppService<AppStrings>().get(Res.string.loading))
        try {
            viewModel.setGroupVisibility(group, visible)
        } finally {
            loading.dismiss()
        }
    }
}

/** The sort method's name, as shown in the sort menu. */
fun MapSortMethod.label(strings: AppStrings = getAppService()): String = strings.get(labelResource())

private fun MapSortMethod.labelResource(): StringResource {
    return when (this) {
        MapSortMethod.MostRecent -> Res.string.most_recent
        MapSortMethod.Closest -> Res.string.closest
        MapSortMethod.Name -> Res.string.name
    }
}

/** The offline map list: the open group, its maps (searched and sorted) and the map changes that need no prompt. */
class OfflineMapListViewModel(
    val mapService: MapService,
    private val prefs: PhotoMapPreferences,
    private val locationProvider: () -> Coordinate
) : ViewModel() {

    var items by mutableStateOf<List<IMap>>(emptyList())
        private set

    /** Changes when the open group changes, so the list scrolls back to the top. */
    var resetKey by mutableIntStateOf(0)
        private set

    /** The open group's name, or null at the top level. */
    var rootName by mutableStateOf<String?>(null)
        private set
    var searchText by mutableStateOf("")
        private set
    var sort by mutableStateOf(prefs.mapSort)
        private set
    var isCreateEnabled by mutableStateOf(true)

    private var isImportClaimed = false

    private val manager = GroupListManager(
        viewModelScope,
        MapGroupLoader(mapService.loader),
        null,
        this::sortMaps
    ).apply {
        onChange = { root, items, rootChanged ->
            this@OfflineMapListViewModel.items = items
            if (rootChanged) {
                resetKey++
            }
            rootName = (root as MapGroup?)?.name
        }
    }

    val rootId: Long?
        get() = manager.root?.id

    fun onResume(cleanup: suspend () -> Boolean) {
        manager.refresh()
        viewModelScope.launch {
            val mapsDeleted = cleanup()
            if (mapsDeleted) {
                manager.refresh()
            }
        }
    }

    fun refresh(resetScroll: Boolean = false) {
        manager.refresh(resetScroll)
    }

    fun search(text: String) {
        searchText = text
        manager.search(text)
    }

    fun open(group: MapGroup) {
        manager.open(group.id)
    }

    /** Leaves the open group; false at the top level. */
    fun up(): Boolean {
        return manager.up()
    }

    fun changeSort(method: MapSortMethod) {
        prefs.mapSort = method
        sort = method
        manager.refresh(true)
    }

    /** True the first time only: a shared map is imported once even if the screen is recreated. */
    fun claimImport(): Boolean {
        if (isImportClaimed) return false
        isImportClaimed = true
        return true
    }

    suspend fun save(map: IMap) {
        onIO {
            mapService.add(map)
        }
    }

    fun toggleVisibility(map: IMap) {
        viewModelScope.launch {
            ToggleVisibilityMapCommand(mapService).execute(map)
            manager.refresh()
        }
    }

    suspend fun copyToAppStorage(map: VectorMap) {
        mapService.copyToAppStorage(map)
        manager.refresh()
    }

    suspend fun setGroupVisibility(group: MapGroup, visible: Boolean) {
        onIO {
            val maps = mapService.loader.getChildren(group.id, null)

            // Photo Maps
            maps
                .asSequence()
                .filterIsInstance<PhotoMap>()
                .filter { it.visible != visible }
                .forEach { mapService.add(it.copy(visible = visible)) }

            // Vector Maps
            maps
                .asSequence()
                .filterIsInstance<VectorMap>()
                .filter { it.visible != visible }
                .forEach { mapService.add(it.copy(visible = visible)) }
        }
        manager.refresh()
    }

    private suspend fun sortMaps(maps: List<IMap>): List<IMap> = onDefault {
        val strategy = when (sort) {
            MapSortMethod.Closest -> ClosestMapSortStrategy(locationProvider(), mapService.loader)
            MapSortMethod.MostRecent -> MostRecentMapSortStrategy(mapService.loader)
            MapSortMethod.Name -> NameMapSortStrategy()
        }

        strategy.sort(maps)
    }
}

@Composable
fun OfflineMapListScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IMap>,
    isEmpty: Boolean,
    resetKey: Int,
    isCreateMenuOpen: Boolean,
    isCreateEnabled: Boolean,
    canPrint: Boolean,
    gpsLocationOnMap: (IMap) -> Boolean,
    onSearchChange: (String) -> Unit,
    onToolbarAction: (OfflineMapToolbarAction) -> Unit,
    onCreateMenuOpenChange: (Boolean) -> Unit,
    onCreateAction: (OfflineMapCreateAction) -> Unit,
    onPhotoMapAction: (PhotoMap, MapAction) -> Unit,
    onVectorMapAction: (VectorMap, VectorMapAction) -> Unit,
    onMapGroupAction: (MapGroup, MapGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    Box(modifier = modifier.testTag("map_list_title")) {
        TrailSensePage(
            title = title,
            backLabel = appString(Res.string.back),
            state = listState,
            contentPadding = PaddingValues(
                start = TrailSenseSpacing.lg,
                end = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.sm,
                bottom = 112.dp
            ),
            actions = {
                OfflineMapListMenu(
                    sortMenuText = sortMenuText,
                    showSort = !isEmpty,
                    onToolbarAction = onToolbarAction
                )
            },
            floatingActionButton = {
                if (!isEmpty) {
                    OfflineMapCreateButton(
                        isOpen = isCreateMenuOpen,
                        enabled = isCreateEnabled,
                        onOpenChange = onCreateMenuOpenChange,
                        onAction = onCreateAction
                    )
                } else if (isCreateMenuOpen) {
                    // No FAB beside the empty state: its one button opens the same add sheet.
                    OfflineMapCreateSheet(
                        onDismiss = { onCreateMenuOpenChange(false) },
                        onAction = onCreateAction
                    )
                }
            }
        ) {
            if (isEmpty) {
                item(key = "empty") {
                    TrailSenseEmptyState(
                        icon = TrailIcons.Maps,
                        illustration = { OfflineMapsEmptyIllustration() },
                        title = appString(Res.string.no_offline_maps),
                        description = appString(Res.string.map_empty_description),
                        modifier = Modifier
                            .fillParentMaxHeight(0.9f)
                            .testTag("map_empty_text"),
                        actionLabel = appString(Res.string.add_offline_map),
                        actionIcon = TrailSenseIcons.Filled.Add,
                        onAction = {
                            if (isCreateEnabled) {
                                onCreateMenuOpenChange(true)
                            }
                        },
                        actionModifier = Modifier.testTag("add_btn")
                    )
                }
                return@TrailSensePage
            }

            // Which of my maps covers where I am? The row you're on says "On map"; no card repeats it above.

            item(key = "search") {
                TrailSenseSearchBar(
                    value = searchText,
                    onValueChange = onSearchChange,
                    placeholder = appString(Res.string.search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("searchbox")
                )
            }

            if (items.isEmpty()) {
                item(key = "no_results") {
                    TrailSenseEmptyState(
                        icon = TrailIcons.Search,
                        title = appString(Res.string.no_results),
                        modifier = Modifier.fillParentMaxHeight(0.9f)
                    )
                }
            }

            placesGroup(
                key = "maps",
                items = items,
                itemKey = { item ->
                    when (item) {
                        is PhotoMap -> "p${item.id}"
                        is VectorMap -> "v${item.id}"
                        is MapGroup -> "g${item.id}"
                        else -> "o${item.id}"
                    }
                }
            ) { item, position, rowModifier ->
                val tagged = if (position.isFirst) rowModifier.testTag("map_list") else rowModifier
                when (item) {
                    is PhotoMap -> PhotoMapRow(
                        map = item,
                        onMap = gpsLocationOnMap(item),
                        canPrint = canPrint,
                        position = position,
                        modifier = tagged,
                        onAction = { onPhotoMapAction(item, it) }
                    )

                    is VectorMap -> VectorMapRow(
                        map = item,
                        onMap = gpsLocationOnMap(item),
                        position = position,
                        modifier = tagged,
                        onAction = { onVectorMapAction(item, it) }
                    )

                    is MapGroup -> MapGroupRow(
                        group = item,
                        position = position,
                        modifier = tagged,
                        onAction = { onMapGroupAction(item, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineMapListMenu(
    sortMenuText: String,
    showSort: Boolean,
    onToolbarAction: (OfflineMapToolbarAction) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    if (showSort) Box {
        IconButton(
            onClick = { isMenuOpen = true },
            modifier = Modifier.testTag("map_list_menu_btn")
        ) {
            Icon(
                imageVector = TrailSenseIcons.Filled.MoreVert,
                contentDescription = appString(Res.string.sort)
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(sortMenuText) },
                leadingIcon = { Icon(TrailIcons.SortAscending, contentDescription = null) },
                onClick = {
                    isMenuOpen = false
                    onToolbarAction(OfflineMapToolbarAction.Sort)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
private fun OfflineMapCreateButton(
    isOpen: Boolean,
    enabled: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onAction: (OfflineMapCreateAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isOpen) {
            OfflineMapCreateSheet(
                onDismiss = { onOpenChange(false) },
                onAction = onAction
            )
        }
        ExtendedFloatingActionButton(
            onClick = {
                if (enabled) {
                    onOpenChange(!isOpen)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("add_btn"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = {
                Icon(
                    imageVector = if (isOpen) {
                        TrailSenseIcons.Filled.Close
                    } else {
                        TrailSenseIcons.Filled.Add
                    },
                    contentDescription = null
                )
            },
            text = {
                Text(appString(Res.string.add))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
private fun OfflineMapCreateSheet(
    onDismiss: () -> Unit,
    onAction: (OfflineMapCreateAction) -> Unit
) {
    TrailSenseSheet(
        onDismissRequest = onDismiss,
        title = appString(Res.string.add_offline_map)
    ) {
        val entries = listOf(
            Triple(Res.string.map_file, TrailSenseIcons.Filled.FileOpen, OfflineMapCreateAction.ImportFile),
            Triple(Res.string.camera, TrailSenseIcons.Filled.PhotoCamera, OfflineMapCreateAction.ImportCamera),
            Triple(Res.string.blank, TrailSenseIcons.Filled.Draw, OfflineMapCreateAction.CreateBlank),
            Triple(Res.string.group, TrailSenseIcons.Filled.Folder, OfflineMapCreateAction.CreateGroup)
        )
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            entries.forEachIndexed { index, (label, icon, action) ->
                PlacesRow(
                    title = appString(label),
                    position = TrailSenseGroupPosition.at(index, entries.size),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    leading = {
                        com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(icon))
                    },
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@Composable
private fun OfflineMapCreateMenuItem(
    label: StringResource,
    icon: ImageVector,
    onAction: (OfflineMapCreateAction) -> Unit,
    action: OfflineMapCreateAction
) {
    ListItem(
        headlineContent = { Text(appString(label)) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(action) }
    )
}

@Composable
private fun PhotoMapRow(
    map: PhotoMap,
    onMap: Boolean,
    canPrint: Boolean,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (MapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val subtitle = formatter.join(
        appString(Res.string.map_type_photo),
        map.createdOn?.let { formatter.formatDate(it, includeWeekDay = false) },
        separator = FormatService.Separator.Dot
    )

    PlacesRow(
        title = map.name,
        subtitle = subtitle,
        position = position,
        modifier = modifier,
        dimmed = !map.visible,
        leading = { MapBadge(icon = TrailIcons.PhotoMaps, onMap = onMap) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        value = if (onMap) appString(Res.string.on_map) else formatter.formatFileSize(map.fileSizeBytes),
        valueColor = if (onMap) trailSenseStatusColors().good else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = { onAction(MapAction.View) },
        actions = {
            MapVisibilityButton(
                visible = map.visible,
                tag = "map_visibility_toggle_${map.name}",
                onClick = { onAction(MapAction.ToggleVisibility) }
            )
            PhotoMapMenu(
                map = map,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                canPrint = canPrint,
                onAction = onAction
            )
        }
    )
}

/** A map's leading mark: the family well with the map type's icon; green while you're on it. */
@Composable
private fun MapBadge(icon: ImageVector, onMap: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    PlacesBadge(
        // The family's well (40 dp Cookie6, 12% primary) for every map; only "you're on it" changes the mark, so it
        // means something. The icon already says photo or trail map.
        containerColor = if (onMap) status.goodContainer else scheme.primary.copy(alpha = 0.12f),
        contentColor = if (onMap) status.onGoodContainer else scheme.primary,
        icon = icon,
        shape = MaterialShapes.Cookie6Sided.toShape(),
        size = 40.dp,
        iconScale = 0.55f
    )
}

@Composable
private fun PhotoMapMenu(
    map: PhotoMap,
    canPrint: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (MapAction) -> Unit
) {
    Box {
        IconButton(
            onClick = { onMenuOpenChange(true) },
            modifier = Modifier.testTag("photo_map_menu_${map.name}")
        ) {
            Icon(
                imageVector = TrailIcons.MenuDots,
                contentDescription = appString(Res.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { onMenuOpenChange(false) }
        ) {
            PhotoMapMenuItem(Res.string.rename, onMenuOpenChange, onAction, MapAction.Rename)
            PhotoMapMenuItem(Res.string.move_to, onMenuOpenChange, onAction, MapAction.Move)
            PhotoMapMenuItem(Res.string.change_resolution, onMenuOpenChange, onAction, MapAction.Resize)
            PhotoMapMenuItem(Res.string.export, onMenuOpenChange, onAction, MapAction.Export)
            if (canPrint) {
                PhotoMapMenuItem(Res.string.print, onMenuOpenChange, onAction, MapAction.Print)
            }
            PhotoMapMenuItem(Res.string.delete, onMenuOpenChange, onAction, MapAction.Delete)
        }
    }
}

@Composable
private fun PhotoMapMenuItem(
    label: StringResource,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (MapAction) -> Unit,
    action: MapAction
) {
    DropdownMenuItem(
        text = { Text(appString(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun VectorMapRow(
    map: VectorMap,
    onMap: Boolean,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (VectorMapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val externalLabel = if (map.isExternal) appString(Res.string.map_external) else null
    val missingLabel = if (!map.isAvailable) appString(Res.string.map_file_missing) else null
    val subtitle = formatter.join(
        appString(Res.string.map_type_trail),
        formatter.formatDate(map.createdOn, includeWeekDay = false),
        externalLabel,
        missingLabel,
        separator = FormatService.Separator.Dot
    )

    PlacesRow(
        title = map.name,
        subtitle = subtitle,
        position = position,
        modifier = modifier,
        dimmed = !map.visible || !map.isAvailable,
        leading = { MapBadge(icon = TrailIcons.Maps, onMap = onMap) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        value = when {
            !map.isAvailable -> appString(Res.string.map_file_missing)
            onMap -> appString(Res.string.on_map)
            else -> formatter.formatFileSize(map.sizeBytes)
        },
        valueColor = when {
            !map.isAvailable -> MaterialTheme.colorScheme.error
            onMap -> trailSenseStatusColors().good
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        onClick = { onAction(VectorMapAction.View) },
        actions = {
            MapVisibilityButton(
                visible = map.visible,
                tag = "map_visibility_toggle_${map.name}",
                onClick = { onAction(VectorMapAction.ToggleVisibility) }
            )
            VectorMapMenu(
                map = map,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        }
    )
}

@Composable
private fun VectorMapMenu(
    map: VectorMap,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (VectorMapAction) -> Unit
) {
    Box {
        IconButton(
            onClick = { onMenuOpenChange(true) },
            modifier = Modifier.testTag("vector_map_menu_${map.name}")
        ) {
            Icon(
                imageVector = TrailIcons.MenuDots,
                contentDescription = appString(Res.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { onMenuOpenChange(false) }
        ) {
            VectorMapMenuItem(Res.string.rename, onMenuOpenChange, onAction, VectorMapAction.Rename)
            VectorMapMenuItem(Res.string.attribution, onMenuOpenChange, onAction, VectorMapAction.EditAttribution)
            VectorMapMenuItem(Res.string.move_to, onMenuOpenChange, onAction, VectorMapAction.Move)
            // Only a file in Trail Sense's storage is exported; an external one is already the user's file
            if (!map.isExternal && map.isAvailable) {
                VectorMapMenuItem(Res.string.export, onMenuOpenChange, onAction, VectorMapAction.Export)
            }
            if (map.isExternal && map.isAvailable) {
                VectorMapMenuItem(
                    Res.string.copy_to_trail_sense,
                    onMenuOpenChange,
                    onAction,
                    VectorMapAction.CopyToAppStorage
                )
            }
            VectorMapMenuItem(Res.string.delete, onMenuOpenChange, onAction, VectorMapAction.Delete)
        }
    }
}

@Composable
private fun VectorMapMenuItem(
    label: StringResource,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (VectorMapAction) -> Unit,
    action: VectorMapAction
) {
    DropdownMenuItem(
        text = { Text(appString(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun MapVisibilityButton(
    visible: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    ) {
        Icon(
            painter = rememberVectorPainter(
                if (visible) {
                    TrailIcons.Visible
                } else {
                    TrailIcons.NotVisible
                }
            ),
            contentDescription = appString(if (visible) Res.string.hide else Res.string.show),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MapGroupRow(
    group: MapGroup,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (MapGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = group.count ?: 0

    PlacesRow(
        title = group.name,
        subtitle = appPluralString(Res.plurals.map_group_summary, count, count),
        position = position,
        modifier = modifier,
        leading = {
            // The count is in the subtitle, so the mark is a folder rather than the number again.
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(TrailSenseIcons.Filled.Folder))
        },
        onClick = { onAction(MapGroupAction.View) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("map_group_menu_${group.name}")
                ) {
                    Icon(
                        imageVector = TrailIcons.MenuDots,
                        contentDescription = appString(Res.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    MapGroupMenuItem(Res.string.show_all, { menuOpen = it }, onAction, MapGroupAction.ShowAll)
                    MapGroupMenuItem(Res.string.hide_all, { menuOpen = it }, onAction, MapGroupAction.HideAll)
                    MapGroupMenuItem(Res.string.rename, { menuOpen = it }, onAction, MapGroupAction.Rename)
                    MapGroupMenuItem(Res.string.move_to, { menuOpen = it }, onAction, MapGroupAction.Move)
                    MapGroupMenuItem(Res.string.delete, { menuOpen = it }, onAction, MapGroupAction.Delete)
                }
            }
        }
    )
}

@Composable
private fun MapGroupMenuItem(
    label: StringResource,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (MapGroupAction) -> Unit,
    action: MapGroupAction
) {
    DropdownMenuItem(
        text = { Text(appString(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun mapTypeSubtitle(metadata: String, typeLabel: StringResource, onMap: Boolean): String {
    val type = appString(typeLabel)
    return if (onMap) {
        "$metadata · ${appString(Res.string.on_map)} · $type"
    } else {
        "$metadata · $type"
    }
}

enum class OfflineMapToolbarAction {
    Sort
}

enum class OfflineMapCreateAction {
    CreateGroup,
    ImportFile,
    ImportCamera,
    CreateBlank
}

@Preview(showBackground = true)
@Composable
private fun OfflineMapListScreenPreview() {
    TrailSenseTheme {
        OfflineMapListScreen(
            title = "Offline maps",
            sortMenuText = "Sort by Closest",
            searchText = "",
            items = emptyList(),
            isEmpty = true,
            resetKey = 0,
            isCreateMenuOpen = false,
            isCreateEnabled = true,
            canPrint = true,
            gpsLocationOnMap = { false },
            onSearchChange = {},
            onToolbarAction = {},
            onCreateMenuOpenChange = {},
            onCreateAction = {},
            onPhotoMapAction = { _, _ -> },
            onVectorMapAction = { _, _ -> },
            onMapGroupAction = { _, _ -> }
        )
    }
}
