package com.kylecorry.trail_sense.tools.offline_maps.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.os.BundleCompat
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.loading.AlertLoadingIndicator
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.ClosestMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MapSortMethod
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MostRecentMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.NameMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateBlankMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromCameraCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromFileCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromUriCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.ICreateMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.MapCleanupCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.CreateMapGroupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.MoveMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ResizeMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ToggleVisibilityMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapGroupAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.VectorMapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapExportService

@Composable
fun OfflineMapListToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { OfflineMapListController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class OfflineMapListController(private val host: TrailSenseDestinationHost) {

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val mapService by lazy { MapService.Companion.getInstance(host.requireContext()) }
    private val mapLoader by lazy { MapGroupLoader(mapService.loader) }
    private lateinit var manager: GroupListManager<IMap>

    private var sort = MapSortMethod.Closest

    private var lastRoot: IMap? = null
    private var backPressedCallback: OnBackPressedCallback? = null
    private var mapItems by mutableStateOf<List<IMap>>(emptyList())
    private var mapListResetKey by mutableIntStateOf(0)
    private var title by mutableStateOf("")
    private var searchText by mutableStateOf("")
    private var sortMenuText by mutableStateOf("")
    private var isCreateMenuOpen by mutableStateOf(false)
    private var isCreateEnabled by mutableStateOf(true)
    private var isInitialized = false

    private val uriPicker by lazy {
        IntentUriPicker(
            host.intentResultRetriever,
            host.requireContext()
        )
    }
    private val mapImportingIndicator by lazy {
        AlertLoadingIndicator(
            host.requireContext(),
            host.getString(R.string.importing_map)
        )
    }
    private val exportService by lazy { PhotoMapExportService(host) }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        OfflineMapListScreen(
            title = title.ifBlank { stringResource(R.string.offline_maps) },
            sortMenuText = sortMenuText.ifBlank {
                stringResource(R.string.sort_by, stringResource(R.string.closest))
            },
            searchText = searchText,
            items = mapItems,
            isEmpty = mapItems.isEmpty(),
            resetKey = mapListResetKey,
            isCreateMenuOpen = isCreateMenuOpen,
            isCreateEnabled = isCreateEnabled,
            gpsLocationOnMap = { map ->
                when (map) {
                    is PhotoMap -> map.boundary()?.contains(gps.location) ?: false
                    is VectorMap -> map.bounds?.contains(gps.location) ?: false
                    else -> false
                }
            },
            onSearchChange = {
                searchText = it
                manager.search(it)
            },
            onToolbarAction = ::handleToolbarAction,
            onCreateMenuOpenChange = { isCreateMenuOpen = it },
            onCreateAction = ::handleCreateAction,
            onPhotoMapAction = ::onPhotoMapAction,
            onVectorMapAction = ::onVectorMapAction,
            onMapGroupAction = ::onMapGroupAction,
            modifier = modifier
        )
    }

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        manager = GroupListManager(
            host.lifecycleScope,
            mapLoader,
            lastRoot,
            this::sortMaps
        )

        val mapIntentUri =
            BundleCompat.getParcelable(
                host.destinationArguments ?: Bundle(),
                "map_intent_uri",
                Uri::class.java
            )
        host.destinationArguments?.remove("map_intent_uri")
        if (mapIntentUri != null) {
            createMap(
                CreateMapFromUriCommand(
                    host.requireContext(),
                    mapIntentUri,
                    mapImportingIndicator
                )
            )
        }

        sort = prefs.photoMaps.mapSort
        sortMenuText = host.getString(R.string.sort_by, getSortString(sort))
        manager.onChange = { root, items, rootChanged ->
            mapItems = items
            if (rootChanged) {
                mapListResetKey++
            }
            title = (root as MapGroup?)?.name ?: host.getString(R.string.offline_maps)
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!manager.up()) {
                    remove()
                    host.findNavController().navigateUp()
                }
            }
        }
        host.mainActivity.onBackPressedDispatcher.addCallback(
            host.lifecycleOwner,
            backPressedCallback!!
        )
        updateBackPressedCallback()
        title = host.getString(R.string.offline_maps)
    }

    fun onResume() {
        if (!isInitialized) {
            return
        }
        updateBackPressedCallback()
        manager.refresh()
        host.inBackground {
            val mapsDeleted = MapCleanupCommand(host.requireContext()).execute()
            if (mapsDeleted) {
                manager.refresh()
            }
        }
    }

    fun onDestroy() {
        onPause()
        backPressedCallback?.remove()
        backPressedCallback = null
        isInitialized = false
    }

    private fun changeSort() {
        val sortOptions = MapSortMethod.values()
        Pickers.item(
            host.requireContext(),
            host.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.photoMaps.mapSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.photoMaps.mapSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                sortMenuText = host.getString(R.string.sort_by, getSortString(sort))
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: MapSortMethod): String {
        return when (sortMethod) {
            MapSortMethod.MostRecent -> host.getString(R.string.most_recent)
            MapSortMethod.Closest -> host.getString(R.string.closest)
            MapSortMethod.Name -> host.getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private suspend fun sortMaps(maps: List<IMap>): List<IMap> = onDefault {
        val strategy = when (sort) {
            MapSortMethod.Closest -> ClosestMapSortStrategy(gps.location, mapService.loader)
            MapSortMethod.MostRecent -> MostRecentMapSortStrategy(mapService.loader)
            MapSortMethod.Name -> NameMapSortStrategy()
        }

        strategy.sort(maps)
    }

    private fun onMapGroupAction(group: MapGroup, action: MapGroupAction) {
        when (action) {
            MapGroupAction.View -> view(group)
            MapGroupAction.Delete -> delete(group)
            MapGroupAction.Rename -> rename(group)
            MapGroupAction.Move -> move(group)
            MapGroupAction.ShowAll -> setGroupVisibility(group, true)
            MapGroupAction.HideAll -> setGroupVisibility(group, false)
        }
    }

    private fun setGroupVisibility(group: MapGroup, visible: Boolean) {
        host.inBackground {
            Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
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

                onMain {
                    manager.refresh()
                }
            }
        }
    }

    private fun onPhotoMapAction(map: PhotoMap, action: MapAction) {
        when (action) {
            MapAction.View -> view(map)
            MapAction.Delete -> delete(map)
            MapAction.Export -> export(map)
            MapAction.Print -> print(map)
            MapAction.Resize -> resize(map)
            MapAction.Rename -> rename(map)
            MapAction.Move -> move(map)
            MapAction.ToggleVisibility -> toggleVisibility(map)
        }
    }

    private fun onVectorMapAction(map: VectorMap, action: VectorMapAction) {
        when (action) {
            VectorMapAction.View -> view(map)
            VectorMapAction.Rename -> rename(map)
            VectorMapAction.EditAttribution -> editAttribution(map)
            VectorMapAction.Delete -> delete(map)
            VectorMapAction.Move -> move(map)
            VectorMapAction.ToggleVisibility -> toggleVisibility(map)
            VectorMapAction.CopyToAppStorage -> copyToAppStorage(map)
        }
    }

    private fun copyToAppStorage(map: VectorMap) {
        host.inBackground {
            mapImportingIndicator.show()
            try {
                mapService.copyToAppStorage(map)
            } finally {
                mapImportingIndicator.hide()
            }
            manager.refresh()
        }
    }

    private fun resize(map: PhotoMap) {
        host.inBackground {
            ResizeMapCommand(host.requireContext(), mapImportingIndicator).execute(map)
            manager.refresh()
        }
    }

    private fun print(map: PhotoMap) {
        host.inBackground(BackgroundMinimumState.Created) {
            PrintMapCommand(host.requireContext()).execute(map)
        }
    }

    private fun export(map: PhotoMap) {
        exportService.export(map)
    }

    private fun rename(map: IMap) {
        host.inBackground {
            RenameMapCommand(host.requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun editAttribution(map: VectorMap) {
        host.inBackground {
            EditOfflineMapAttributionCommand(host.requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun move(map: IMap) {
        host.inBackground {
            MoveMapCommand(host.requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun toggleVisibility(map: IMap) {
        host.inBackground {
            ToggleVisibilityMapCommand(mapService).execute(map)
            manager.refresh()
        }
    }

    private fun delete(map: IMap) {
        host.inBackground {
            DeleteMapCommand(host.requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun view(map: IMap) {
        when (map) {
            is MapGroup -> manager.open(map.id)
            is PhotoMap -> host.findNavController().navigate(
                R.id.action_mapList_to_maps,
                Bundle().apply {
                    putLong("mapId", map.id)
                }
            )

            is VectorMap -> {
                if (!map.isAvailable) {
                    Alerts.dialog(
                        host.requireContext(),
                        host.getString(R.string.invalid_trail_map),
                        host.getString(R.string.invalid_trail_map_message),
                        cancelText = null
                    )
                    return
                }
                host.findNavController().navigateWithAnimation(
                    R.id.offlineMapViewFragment,
                    Bundle().apply {
                        putLong("offline_map_file_id", map.id)
                    }
                )
            }
        }
    }

    private fun handleToolbarAction(action: OfflineMapToolbarAction) {
        when (action) {
            OfflineMapToolbarAction.Sort -> changeSort()
        }
    }

    private fun handleCreateAction(action: OfflineMapCreateAction) {
        when (action) {
            OfflineMapCreateAction.CreateGroup -> createMapGroup()
            OfflineMapCreateAction.ImportFile -> createMap(
                CreateMapFromFileCommand(
                    host.requireContext(),
                    uriPicker,
                    mapImportingIndicator
                )
            )

            OfflineMapCreateAction.ImportCamera -> createMap(
                CreateMapFromCameraCommand(
                    host,
                    mapImportingIndicator
                )
            )

            OfflineMapCreateAction.CreateBlank -> createMap(
                CreateBlankMapCommand(
                    host.requireContext(),
                    mapImportingIndicator
                )
            )
        }
        isCreateMenuOpen = false
    }

    private fun createMapGroup() {
        host.inBackground {
            CreateMapGroupCommand(host.requireContext(), mapService).execute(manager.root?.id)
            manager.refresh()
        }
    }

    fun onPause() {
        if (!isInitialized) {
            return
        }
        tryOrNothing {
            lastRoot = manager.root
        }
    }

    private fun updateBackPressedCallback() {
        backPressedCallback?.isEnabled = isInitialized
    }

    private fun createMap(command: ICreateMapCommand) {
        host.inBackground(BackgroundMinimumState.Created) {
            MapCleanupCommand.isMapImportInProgress = true
            try {
                onMain {
                    isCreateEnabled = false
                }

                val map = command.execute()?.let {
                    when (it) {
                        is PhotoMap -> it.copy(parentId = manager.root?.id)
                        is VectorMap -> it.copy(parentId = manager.root?.id)
                        else -> null
                    }
                }

                if (map == null) {
                    host.toast(host.getString(R.string.error_importing_map))
                    return@inBackground
                }

                if (map.parentId != null) {
                    onIO {
                        mapService.add(map)
                    }
                }

                if (map is PhotoMap) {
                    val isPdfMap = map.hasPdf(host.requireContext())
                    if ((isPdfMap && prefs.photoMaps.autoReducePdfMaps) || (!isPdfMap && prefs.photoMaps.autoReducePhotoMaps)) {
                        mapImportingIndicator.show()
                        val reducer = HighQualityMapReducer(host.requireContext())
                        reducer.reduce(map)
                        mapImportingIndicator.hide()
                    }

                    if (map.georeference.calibrationPoints.isNotEmpty()) {
                        host.toast(host.getString(R.string.map_auto_calibrated))
                    }
                }

                manager.refresh(true)
                when (map) {
                    is PhotoMap -> host.findNavController().navigate(
                        R.id.action_mapList_to_maps,
                        Bundle().apply {
                            putLong("mapId", map.id)
                        }
                    )
                }
            } finally {
                MapCleanupCommand.isMapImportInProgress = false
                onMain {
                    isCreateEnabled = true
                }
            }

        }
    }


}

@Composable
private fun OfflineMapListScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IMap>,
    isEmpty: Boolean,
    resetKey: Int,
    isCreateMenuOpen: Boolean,
    isCreateEnabled: Boolean,
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                OfflineMapListHeader(
                    title = title,
                    sortMenuText = sortMenuText,
                    showSort = !isEmpty,
                    onToolbarAction = onToolbarAction,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isEmpty) {
                    TrailSenseSearchBar(
                        value = searchText,
                        onValueChange = onSearchChange,
                        placeholder = stringResource(R.string.search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TrailSenseSpacing.screen)
                            .testTag("searchbox")
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = TrailSenseSpacing.lg)
                ) {
                    if (isEmpty) {
                        TrailSenseEmptyState(
                            icon = TrailSenseIcons.Filled.Map,
                            title = stringResource(R.string.no_offline_maps),
                            description = stringResource(R.string.map_empty_description),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("map_empty_text")
                        )
                    } else {
                        OfflineMapList(
                            items = items,
                            resetKey = resetKey,
                            gpsLocationOnMap = gpsLocationOnMap,
                            onPhotoMapAction = onPhotoMapAction,
                            onVectorMapAction = onVectorMapAction,
                            onMapGroupAction = onMapGroupAction,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            OfflineMapCreateButton(
                isOpen = isCreateMenuOpen,
                enabled = isCreateEnabled,
                onOpenChange = onCreateMenuOpenChange,
                onAction = onCreateAction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TrailSenseSpacing.screen)
            )
        }
    }
}

@Composable
private fun OfflineMapListHeader(
    title: String,
    sortMenuText: String,
    showSort: Boolean,
    onToolbarAction: (OfflineMapToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(start = TrailSenseSpacing.screen, end = TrailSenseSpacing.md)
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("map_list_title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (showSort) Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.testTag("map_list_menu_btn")
            ) {
                Icon(
                    imageVector = TrailSenseIcons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.sort)
                )
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(sortMenuText) },
                    onClick = {
                        isMenuOpen = false
                        onToolbarAction(OfflineMapToolbarAction.Sort)
                    }
                )
            }
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
                Text(stringResource(R.string.add))
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.add_offline_map),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.xl,
                vertical = TrailSenseSpacing.md
            )
        )
        OfflineMapCreateMenuItem(
            R.string.map_file, TrailSenseIcons.Filled.FileOpen,
            onAction, OfflineMapCreateAction.ImportFile
        )
        OfflineMapCreateMenuItem(
            R.string.camera, TrailSenseIcons.Filled.PhotoCamera,
            onAction, OfflineMapCreateAction.ImportCamera
        )
        OfflineMapCreateMenuItem(
            R.string.blank, TrailSenseIcons.Filled.Draw,
            onAction, OfflineMapCreateAction.CreateBlank
        )
        OfflineMapCreateMenuItem(
            R.string.group, TrailSenseIcons.Filled.Folder,
            onAction, OfflineMapCreateAction.CreateGroup
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.xl))
    }
}

@Composable
private fun OfflineMapCreateMenuItem(
    label: Int,
    icon: ImageVector,
    onAction: (OfflineMapCreateAction) -> Unit,
    action: OfflineMapCreateAction
) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(action) }
    )
}

@Composable
private fun OfflineMapList(
    items: List<IMap>,
    resetKey: Int,
    gpsLocationOnMap: (IMap) -> Boolean,
    onPhotoMapAction: (PhotoMap, MapAction) -> Unit,
    onVectorMapAction: (VectorMap, VectorMapAction) -> Unit,
    onMapGroupAction: (MapGroup, MapGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("map_list")
    ) {
        items(items, key = { item ->
            when (item) {
                is PhotoMap -> item.id
                is VectorMap -> item.id + 10000
                is MapGroup -> -item.id
                else -> item.id
            }
        }) { item ->
            when (item) {
                is PhotoMap -> PhotoMapRow(
                    map = item,
                    onMap = gpsLocationOnMap(item),
                    onAction = { onPhotoMapAction(item, it) }
                )

                is VectorMap -> VectorMapRow(
                    map = item,
                    onMap = gpsLocationOnMap(item),
                    onAction = { onVectorMapAction(item, it) }
                )

                is MapGroup -> MapGroupRow(
                    group = item,
                    onAction = { onMapGroupAction(item, it) }
                )
            }
            TrailSenseDivider()
        }
    }
}

@Composable
private fun PhotoMapRow(
    map: PhotoMap,
    onMap: Boolean,
    onAction: (MapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val subtitle = formatter.join(
        map.createdOn?.let { formatter.formatDate(it.toZonedDateTime(), includeWeekDay = false) },
        formatter.formatFileSize(map.fileSizeBytes),
        separator = FormatService.Separator.Dot
    )

    TrailSenseListItem(
        title = map.name,
        subtitle = mapTypeSubtitle(subtitle, R.string.map_type_photo, onMap),
        icon = painterResource(R.drawable.maps),
        iconTint = Color(AppColor.Gray.color),
        onClick = { onAction(MapAction.View) },
        trailing = {
            MapVisibilityButton(
                visible = map.visible,
                tag = "map_visibility_toggle_${map.name}",
                onClick = { onAction(MapAction.ToggleVisibility) }
            )
            PhotoMapMenu(
                map = map,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.screen)
    )
}

@Composable
private fun PhotoMapMenu(
    map: PhotoMap,
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
                painter = painterResource(R.drawable.ic_menu_dots),
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { onMenuOpenChange(false) }
        ) {
            PhotoMapMenuItem(R.string.rename, onMenuOpenChange, onAction, MapAction.Rename)
            PhotoMapMenuItem(R.string.move_to, onMenuOpenChange, onAction, MapAction.Move)
            PhotoMapMenuItem(R.string.change_resolution, onMenuOpenChange, onAction, MapAction.Resize)
            PhotoMapMenuItem(R.string.export, onMenuOpenChange, onAction, MapAction.Export)
            if (Printer.canPrint()) {
                PhotoMapMenuItem(R.string.print, onMenuOpenChange, onAction, MapAction.Print)
            }
            PhotoMapMenuItem(R.string.delete, onMenuOpenChange, onAction, MapAction.Delete)
        }
    }
}

@Composable
private fun PhotoMapMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (MapAction) -> Unit,
    action: MapAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
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
    onAction: (VectorMapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val externalLabel = if (map.isExternal) stringResource(R.string.map_external) else null
    val missingLabel = if (!map.isAvailable) stringResource(R.string.map_file_missing) else null
    val subtitle = formatter.join(
        formatter.formatDate(map.createdOn.toZonedDateTime(), includeWeekDay = false),
        formatter.formatFileSize(map.sizeBytes),
        externalLabel,
        missingLabel,
        separator = FormatService.Separator.Dot
    )

    TrailSenseListItem(
        title = map.name,
        subtitle = mapTypeSubtitle(subtitle, R.string.map_type_trail, onMap),
        icon = painterResource(R.drawable.maps),
        iconTint = Color(AppColor.Gray.color),
        onClick = { onAction(VectorMapAction.View) },
        trailing = {
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
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.screen)
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
                painter = painterResource(R.drawable.ic_menu_dots),
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { onMenuOpenChange(false) }
        ) {
            VectorMapMenuItem(R.string.rename, onMenuOpenChange, onAction, VectorMapAction.Rename)
            VectorMapMenuItem(R.string.attribution, onMenuOpenChange, onAction, VectorMapAction.EditAttribution)
            VectorMapMenuItem(R.string.move_to, onMenuOpenChange, onAction, VectorMapAction.Move)
            if (map.isExternal && map.isAvailable) {
                VectorMapMenuItem(
                    R.string.copy_to_trail_sense,
                    onMenuOpenChange,
                    onAction,
                    VectorMapAction.CopyToAppStorage
                )
            }
            VectorMapMenuItem(R.string.delete, onMenuOpenChange, onAction, VectorMapAction.Delete)
        }
    }
}

@Composable
private fun VectorMapMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (VectorMapAction) -> Unit,
    action: VectorMapAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
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
            painter = painterResource(
                if (visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                }
            ),
            contentDescription = stringResource(if (visible) R.string.hide else R.string.show),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MapGroupRow(
    group: MapGroup,
    onAction: (MapGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = group.count ?: 0

    TrailSenseListItem(
        title = group.name,
        subtitle = stringResource(R.plurals.map_group_summary, count, count),
        icon = painterResource(R.drawable.ic_map_group),
        iconTint = Color(AppColor.Gray.color),
        onClick = { onAction(MapGroupAction.View) },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("map_group_menu_${group.name}")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_dots),
                        contentDescription = stringResource(R.string.details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    MapGroupMenuItem(R.string.show_all, { menuOpen = it }, onAction, MapGroupAction.ShowAll)
                    MapGroupMenuItem(R.string.hide_all, { menuOpen = it }, onAction, MapGroupAction.HideAll)
                    MapGroupMenuItem(R.string.rename, { menuOpen = it }, onAction, MapGroupAction.Rename)
                    MapGroupMenuItem(R.string.move_to, { menuOpen = it }, onAction, MapGroupAction.Move)
                    MapGroupMenuItem(R.string.delete, { menuOpen = it }, onAction, MapGroupAction.Delete)
                }
            }
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.screen)
    )
}

@Composable
private fun MapGroupMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (MapGroupAction) -> Unit,
    action: MapGroupAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun mapTypeSubtitle(metadata: String, typeLabel: Int, onMap: Boolean): String {
    val type = stringResource(typeLabel)
    return if (onMap) {
        "$metadata · ${stringResource(R.string.on_map)} · $type"
    } else {
        "$metadata · $type"
    }
}

private enum class OfflineMapToolbarAction {
    Sort
}

private enum class OfflineMapCreateAction {
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
