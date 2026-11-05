@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.core.net.toUri

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.foundation.layout.Arrangement
import com.kylecorry.trail_sense.shared.views.OfflineMapsEmptyIllustration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTopBar
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

@Composable
fun OfflineMapListToolScreen(
    route: Route.OfflineMapList,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(route) {
        OfflineMapListController(
            context,
            navigator,
            effects,
            lifecycleOwner,
            // The file picker needs the activity's result launcher (an Activity-only API).
            requireNotNull(context.findMainActivity()),
            route.importUri
        )
    }

    // Back leaves the open group first, then the screen.
    BackHandler(enabled = controller.isBackEnabled) {
        if (!controller.up()) {
            navigator.navigateUp()
        }
    }

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

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}

private class OfflineMapListController(
    private val context: Context,
    private val navigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner,
    private val intentResultRetriever: IntentResultRetriever,
    private val importUri: String?
) {

    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(context) }
    private val mapService by lazy { MapService.Companion.getInstance(context) }
    private val mapLoader by lazy { MapGroupLoader(mapService.loader) }
    private lateinit var manager: GroupListManager<IMap>

    private var sort = MapSortMethod.Closest

    private var lastRoot: IMap? = null
    var isBackEnabled by mutableStateOf(false)
        private set
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
            intentResultRetriever,
            context
        )
    }
    private val mapImportingIndicator by lazy {
        AlertLoadingIndicator(
            context,
            context.getString(R.string.importing_map)
        )
    }
    private val exportService by lazy { PhotoMapExportService(context, intentResultRetriever, effects, lifecycleOwner) }

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
            lifecycleOwner.lifecycleScope,
            mapLoader,
            lastRoot,
            this::sortMaps
        )

        val mapIntentUri = importUri?.toUri()
        if (mapIntentUri != null) {
            // Imported once: without the URI, coming back to this screen doesn't import it again.
            navigator.replace(Route.OfflineMapList())
            createMap(
                CreateMapFromUriCommand(
                    context,
                    mapIntentUri,
                    mapImportingIndicator
                )
            )
        }

        sort = prefs.photoMaps.mapSort
        sortMenuText = context.getString(R.string.sort_by, getSortString(sort))
        manager.onChange = { root, items, rootChanged ->
            mapItems = items
            if (rootChanged) {
                mapListResetKey++
            }
            title = (root as MapGroup?)?.name ?: context.getString(R.string.offline_maps)
        }

        updateBackPressedCallback()
        title = context.getString(R.string.offline_maps)
    }

    fun onResume() {
        if (!isInitialized) {
            return
        }
        updateBackPressedCallback()
        manager.refresh()
        lifecycleOwner.inBackground {
            val mapsDeleted = MapCleanupCommand(context).execute()
            if (mapsDeleted) {
                manager.refresh()
            }
        }
    }

    fun onDestroy() {
        onPause()
        isInitialized = false
        updateBackPressedCallback()
    }

    private fun changeSort() {
        val sortOptions = MapSortMethod.values()
        Pickers.item(
            context,
            context.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.photoMaps.mapSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.photoMaps.mapSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                sortMenuText = context.getString(R.string.sort_by, getSortString(sort))
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: MapSortMethod): String {
        return when (sortMethod) {
            MapSortMethod.MostRecent -> context.getString(R.string.most_recent)
            MapSortMethod.Closest -> context.getString(R.string.closest)
            MapSortMethod.Name -> context.getString(R.string.name)
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
        lifecycleOwner.inBackground {
            Alerts.withLoading(context, context.getString(R.string.loading)) {
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
        lifecycleOwner.inBackground {
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
        lifecycleOwner.inBackground {
            ResizeMapCommand(context, mapImportingIndicator).execute(map)
            manager.refresh()
        }
    }

    private fun print(map: PhotoMap) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
            PrintMapCommand(context).execute(map)
        }
    }

    private fun export(map: PhotoMap) {
        exportService.export(map)
    }

    private fun rename(map: IMap) {
        lifecycleOwner.inBackground {
            RenameMapCommand(context, mapService).execute(map)
            manager.refresh()
        }
    }

    private fun editAttribution(map: VectorMap) {
        lifecycleOwner.inBackground {
            EditOfflineMapAttributionCommand(context).execute(map)
            manager.refresh()
        }
    }

    private fun move(map: IMap) {
        lifecycleOwner.inBackground {
            MoveMapCommand(context, mapService).execute(map)
            manager.refresh()
        }
    }

    private fun toggleVisibility(map: IMap) {
        lifecycleOwner.inBackground {
            ToggleVisibilityMapCommand(mapService).execute(map)
            manager.refresh()
        }
    }

    private fun delete(map: IMap) {
        lifecycleOwner.inBackground {
            DeleteMapCommand(context, mapService).execute(map)
            manager.refresh()
        }
    }

    private fun view(map: IMap) {
        when (map) {
            is MapGroup -> manager.open(map.id)
            is PhotoMap -> navigator.navigate(Route.PhotoMaps(mapId = map.id))

            is VectorMap -> {
                if (!map.isAvailable) {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.invalid_trail_map),
                        context.getString(R.string.invalid_trail_map_message),
                        cancelText = null
                    )
                    return
                }
                navigator.navigate(Route.VectorMapView(offlineMapFileId = map.id))
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
                    context,
                    uriPicker,
                    mapImportingIndicator
                )
            )

            OfflineMapCreateAction.ImportCamera -> createMap(
                CreateMapFromCameraCommand(
                    context,
                    effects,
                    mapImportingIndicator
                )
            )

            OfflineMapCreateAction.CreateBlank -> createMap(
                CreateBlankMapCommand(
                    context,
                    mapImportingIndicator
                )
            )
        }
        isCreateMenuOpen = false
    }

    private fun createMapGroup() {
        lifecycleOwner.inBackground {
            CreateMapGroupCommand(context, mapService).execute(manager.root?.id)
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
        isBackEnabled = isInitialized
    }

    /** Leaves the open group; false at the root. */
    fun up(): Boolean {
        return manager.up()
    }

    private fun createMap(command: ICreateMapCommand) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
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
                    effects.showMessage(context.getString(R.string.error_importing_map))
                    return@inBackground
                }

                if (map.parentId != null) {
                    onIO {
                        mapService.add(map)
                    }
                }

                if (map is PhotoMap) {
                    val isPdfMap = map.hasPdf(context)
                    if ((isPdfMap && prefs.photoMaps.autoReducePdfMaps) || (!isPdfMap && prefs.photoMaps.autoReducePhotoMaps)) {
                        mapImportingIndicator.show()
                        val reducer = HighQualityMapReducer(context)
                        reducer.reduce(map)
                        mapImportingIndicator.hide()
                    }

                    if (map.georeference.calibrationPoints.isNotEmpty()) {
                        effects.showMessage(context.getString(R.string.map_auto_calibrated))
                    }
                }

                manager.refresh(true)
                when (map) {
                    is PhotoMap -> navigator.navigate(Route.PhotoMaps(mapId = map.id))
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
    val listState = rememberLazyListState()
    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    Box(modifier = modifier.testTag("map_list_title")) {
        TrailSensePage(
            title = title,
            backLabel = stringResource(R.string.back),
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
                        title = stringResource(R.string.no_offline_maps),
                        description = stringResource(R.string.map_empty_description),
                        modifier = Modifier
                            .fillParentMaxHeight(0.9f)
                            .testTag("map_empty_text"),
                        actionLabel = stringResource(R.string.add_offline_map),
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
                    placeholder = stringResource(R.string.search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("searchbox")
                )
            }

            if (items.isEmpty()) {
                item(key = "no_results") {
                    TrailSenseEmptyState(
                        icon = TrailIcons.Search,
                        title = stringResource(R.string.no_results),
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
                contentDescription = stringResource(R.string.sort)
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
    TrailSenseSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.add_offline_map)
    ) {
        val entries = listOf(
            Triple(R.string.map_file, TrailSenseIcons.Filled.FileOpen, OfflineMapCreateAction.ImportFile),
            Triple(R.string.camera, TrailSenseIcons.Filled.PhotoCamera, OfflineMapCreateAction.ImportCamera),
            Triple(R.string.blank, TrailSenseIcons.Filled.Draw, OfflineMapCreateAction.CreateBlank),
            Triple(R.string.group, TrailSenseIcons.Filled.Folder, OfflineMapCreateAction.CreateGroup)
        )
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            entries.forEachIndexed { index, (label, icon, action) ->
                PlacesRow(
                    title = stringResource(label),
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
private fun PhotoMapRow(
    map: PhotoMap,
    onMap: Boolean,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (MapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val subtitle = formatter.join(
        stringResource(R.string.map_type_photo),
        map.createdOn?.let { formatter.formatDate(it.toZonedDateTime(), includeWeekDay = false) },
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
        value = if (onMap) stringResource(R.string.on_map) else formatter.formatFileSize(map.fileSizeBytes),
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
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (VectorMapAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = getAppService<FormatService>()
    val externalLabel = if (map.isExternal) stringResource(R.string.map_external) else null
    val missingLabel = if (!map.isAvailable) stringResource(R.string.map_file_missing) else null
    val subtitle = formatter.join(
        stringResource(R.string.map_type_trail),
        formatter.formatDate(map.createdOn.toZonedDateTime(), includeWeekDay = false),
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
            !map.isAvailable -> stringResource(R.string.map_file_missing)
            onMap -> stringResource(R.string.on_map)
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
            painter = rememberVectorPainter(
                if (visible) {
                    TrailIcons.Visible
                } else {
                    TrailIcons.NotVisible
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
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (MapGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = group.count ?: 0

    PlacesRow(
        title = group.name,
        subtitle = pluralStringResource(R.plurals.map_group_summary, count, count),
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
        }
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
