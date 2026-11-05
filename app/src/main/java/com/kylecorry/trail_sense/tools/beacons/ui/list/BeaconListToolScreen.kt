@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui.list

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.kylecorry.trail_sense.shared.navigation.toGeoUri

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation


import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.tools.beacons.ui.polygon
import com.kylecorry.trail_sense.shared.views.PlacesEmptyIllustration
import androidx.compose.foundation.layout.size
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.extensions.filterIndices
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.CreateBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.RenameBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell.NavigateToNearestCellSignal
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.MostRecentBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.ui.QRImportContent
import java.time.Instant
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

interface BeaconListControllerHost {
    fun refreshBeaconList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconListToolScreen(
    route: Route.BeaconList,
    modifier: Modifier = Modifier,
    onController: (BeaconListControllerHost?) -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        BeaconListViewModel(
            route = route,
            beaconService = BeaconService(appContext),
            preferences = UserPreferences(appContext).beacons,
            gps = SensorService(appContext).getGPS(),
            importer = BeaconGpxImporter(appContext)
        )
    }
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val haptics = remember(appContext) { HapticSubsystem.getInstance(appContext) }
    val gpxService = remember(context) {
        IOFactory().createGpxService(
            context,
            // File pickers need the activity's result launcher, an Activity-only API.
            requireNotNull(context.findMainActivity())
        )
    }

    // The QR import camera is a rendering object, so it stays with the composition.
    var showQrImport by remember { mutableStateOf(false) }
    var qrImportCamera by remember { mutableStateOf<CameraState?>(null) }
    var qrImportLastMessage by remember { mutableStateOf<String?>(null) }

    fun hideQrImport() {
        if (!showQrImport && qrImportCamera == null) {
            return
        }
        showQrImport = false
        qrImportCamera?.stop()
        qrImportCamera = null
        qrImportLastMessage = null
        haptics.off()
    }

    fun createBeacon(
        group: Long? = null,
        initialLocation: GeoUri? = null,
        editingBeaconId: Long? = null
    ) {
        appNavigator.navigate(
            Route.PlaceBeacon(
                editBeaconId = editingBeaconId ?: 0L,
                initialGroupId = group ?: 0L,
                initialLocation = initialLocation?.toRouteLocation()
            )
        )
    }

    fun onQrImportCameraUpdate(bitmap: Bitmap) {
        if (qrImportCamera == null) {
            bitmap.recycle()
            return
        }

        var message: String? = null
        tryOrNothing {
            message = QR.decode(bitmap)
            bitmap.recycle()
        }

        val decoded = message
        if (decoded == null || qrImportLastMessage == decoded) {
            return
        }

        haptics.click()
        qrImportLastMessage = decoded
        val beacon = BeaconQREncoder().decode(decoded) ?: return
        createBeacon(group = viewModel.rootId, initialLocation = GeoUri.from(beacon))
        hideQrImport()
    }

    fun getSortString(sortMethod: BeaconSortMethod): String {
        return when (sortMethod) {
            BeaconSortMethod.MostRecent -> context.getString(R.string.most_recent)
            BeaconSortMethod.Closest -> context.getString(R.string.closest)
            BeaconSortMethod.Name -> context.getString(R.string.name)
        }
    }

    fun changeSort() {
        val sortOptions = BeaconSortMethod.values()
        Pickers.item(
            context,
            context.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(viewModel.sort)
        ) { newSort ->
            if (newSort != null) {
                viewModel.changeSort(sortOptions[newSort])
            }
        }
    }

    fun export(gpx: GPXData) {
        scope.launch {
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            if (success) {
                effects.showMessage(
                    context.resources.getQuantityString(
                        R.plurals.beacons_exported,
                        gpx.waypoints.size,
                        gpx.waypoints.size
                    )
                )
            } else {
                effects.showMessage(context.getString(R.string.beacon_export_error))
            }
        }
    }

    fun onExportBeacons() {
        scope.launch {
            val gpx = viewModel.getExportGpx()
            Pickers.items(
                context,
                context.getString(R.string.export),
                gpx.waypoints.map { it.name ?: formatService.formatLocation(it.coordinate) },
                List(gpx.waypoints.size) { it },
            ) {
                if (!it.isNullOrEmpty()) {
                    val selectedWaypoints = gpx.waypoints.filterIndices(it)
                    export(gpx.copy(waypoints = selectedWaypoints))
                }
            }
        }
    }

    fun importBeacons() {
        scope.launch {
            val gpx = gpxService.import()
            val waypoints = gpx?.waypoints ?: emptyList()
            Pickers.items(
                context,
                context.getString(R.string.import_btn),
                waypoints.map {
                    it.name ?: formatService.formatLocation(it.coordinate)
                },
                List(waypoints.size) { it }
            ) {
                if (it != null) {
                    scope.launch {
                        val count = viewModel.import(
                            GPXData(waypoints.filterIndices(it), emptyList(), emptyList())
                        )
                        Alerts.toast(
                            context,
                            context.resources.getQuantityString(R.plurals.beacons_imported, count, count)
                        )
                    }
                }
            }
        }
    }

    fun delete(group: BeaconGroup?) {
        Alerts.dialog(
            context,
            context.getString(R.string.delete),
            group?.name?.let {
                context.getString(R.string.delete_beacon_group_message, it)
            } ?: context.getString(R.string.delete_all_beacons),
        ) { cancelled ->
            if (!cancelled) {
                viewModel.delete(group)
            }
        }
    }

    fun setGroupVisibility(group: BeaconGroup, visible: Boolean) {
        scope.launch {
            Alerts.withLoading(context, context.getString(R.string.loading)) {
                viewModel.setGroupVisibility(group, visible)
            }
        }
    }

    fun navigateToNearestCellSignal() {
        scope.launch {
            if (NavigateToNearestCellSignal(appContext).execute()) {
                appNavigator.navigate(Route.Navigation())
            } else {
                effects.showMessage(context.getString(R.string.no_recorded_cell_signal_nearby))
            }
        }
    }

    fun handleToolbarAction(action: BeaconToolbarAction) {
        when (action) {
            BeaconToolbarAction.Sort -> changeSort()
            BeaconToolbarAction.Export -> onExportBeacons()
            BeaconToolbarAction.NavigateToNearestCellSignal -> navigateToNearestCellSignal()
            BeaconToolbarAction.Delete -> delete(viewModel.root as BeaconGroup?)
        }
    }

    fun handleCreateAction(action: BeaconCreateAction) {
        when (action) {
            BeaconCreateAction.ImportQr -> {
                scope.launch {
                    if (effects.requestPermission(AppPermission.Camera)) {
                        showQrImport = true
                        qrImportLastMessage = null
                    } else {
                        effects.explainPermissionDenied(AppPermission.Camera)
                    }
                }
            }

            BeaconCreateAction.ImportGpx -> importBeacons()
            BeaconCreateAction.CreateGroup -> {
                CreateBeaconGroupCommand(context, scope, viewModel.beaconService) {
                    viewModel.refresh()
                }.execute(viewModel.rootId)
            }

            BeaconCreateAction.CreateBeacon -> createBeacon(group = viewModel.rootId)
        }
        viewModel.isCreateMenuOpen = false
    }

    fun handleBeaconGroupAction(group: BeaconGroup, action: BeaconGroupAction) {
        when (action) {
            BeaconGroupAction.Open -> viewModel.open(group.id)
            BeaconGroupAction.ShowAll -> setGroupVisibility(group, true)
            BeaconGroupAction.HideAll -> setGroupVisibility(group, false)
            BeaconGroupAction.Edit -> RenameBeaconGroupCommand(context, scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(group)

            BeaconGroupAction.Delete -> delete(group)
            BeaconGroupAction.Move -> MoveBeaconGroupCommand(context, scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(group)
        }
    }

    fun handleBeaconAction(beacon: Beacon, action: BeaconAction) {
        when (action) {
            BeaconAction.View -> appNavigator.navigate(Route.BeaconDetails(beacon.id))
            BeaconAction.Edit -> createBeacon(editingBeaconId = beacon.id)
            BeaconAction.Navigate -> {
                Navigator.getInstance(appContext).navigateTo(beacon)
                appNavigator.navigate(Route.Navigation())
            }

            BeaconAction.Delete -> DeleteBeaconCommand(context, scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(beacon)

            BeaconAction.Move -> MoveBeaconCommand(context, scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(beacon)

            BeaconAction.ToggleVisibility -> viewModel.toggleVisibility(beacon)
            BeaconAction.Share -> BeaconSender(context, effects, appNavigator, scope).send(beacon)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.createBeaconAt.collect { createBeacon(initialLocation = it) }
    }

    // Back closes the create menu, then goes up a group; at the top it leaves the page as usual.
    BackHandler(enabled = viewModel.handlesBack) {
        if (!viewModel.back()) {
            appNavigator.navigateUp()
        }
    }

    // The activity refreshes the list after a beacon is placed from the quick action.
    DisposableEffect(viewModel) {
        onController(viewModel)
        onDispose { onController(null) }
    }

    // Reloads each time the page is shown, so an edit made in the editor shows on return.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {
            viewModel.onPause()
            hideQrImport()
        }
    }

    BeaconListScreen(
        title = viewModel.groupName ?: stringResource(R.string.places_title),
        sortMenuText = stringResource(R.string.sort_by, getSortString(viewModel.sort)),
        searchText = viewModel.searchText,
        items = viewModel.items,
        loaded = viewModel.isLoaded,
        resetKey = viewModel.resetKey,
        formatService = formatService,
        baseDistanceUnits = prefs.baseDistanceUnits,
        currentLocation = viewModel.location,
        isCreateMenuOpen = viewModel.isCreateMenuOpen,
        onSearchChange = viewModel::search,
        onToolbarAction = ::handleToolbarAction,
        onCreateMenuOpenChange = { viewModel.isCreateMenuOpen = it },
        onCreateAction = ::handleCreateAction,
        onBeaconAction = ::handleBeaconAction,
        onGroupAction = ::handleBeaconGroupAction,
        modifier = modifier.fillMaxSize()
    )
    if (showQrImport) {
        TrailSenseSheet(
            onDismissRequest = { hideQrImport() }
        ) {
            QRImportContent(
                title = stringResource(R.string.beacon_qr_import_instructions),
                onClose = { hideQrImport() },
                onCameraReady = { camera ->
                    qrImportCamera = camera
                    camera.clipToOutline = true
                    camera.start(QR_IMPORT_CAMERA_SIZE) { bitmap ->
                        onQrImportCameraUpdate(bitmap)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The places list's state: a group browser over the beacon service (search, sort, the group you're in), plain Kotlin
 * with no Android UI types. The screen shows the dialogs and file pickers and calls back in; [createBeaconAt] asks it
 * to open the editor at a location the route brought.
 */
internal class BeaconListViewModel(
    route: Route.BeaconList,
    val beaconService: BeaconService,
    private val preferences: IBeaconPreferences,
    private val gps: IGPS,
    private val importer: BeaconGpxImporter
) : ViewModel(), BeaconListControllerHost {

    var items by mutableStateOf<List<IBeacon>>(emptyList())
        private set
    var resetKey by mutableIntStateOf(0)
        private set
    var isLoaded by mutableStateOf(false)
        private set

    /** The open group's name, or null at the top. */
    var groupName by mutableStateOf<String?>(null)
        private set
    var searchText by mutableStateOf("")
        private set
    var sort by mutableStateOf(preferences.beaconSort)
        private set
    var isCreateMenuOpen by mutableStateOf(false)
    private var isInGroup by mutableStateOf(false)

    /** The last known location; read when the list draws, as before. */
    val location: Coordinate
        get() = gps.location

    private val manager = GroupListManager(
        viewModelScope,
        BeaconLoader(beaconService, preferences),
        null,
        this::sortBeacons
    ).also {
        it.onChange = { root, newItems, rootChanged ->
            items = newItems
            isLoaded = true
            if (rootChanged) {
                resetKey++
            }
            isInGroup = root != null
            groupName = root?.name
        }
    }

    val root: IBeacon?
        get() = manager.root

    val rootId: Long?
        get() = manager.root?.id

    private val _createBeaconAt = Channel<GeoUri>(Channel.BUFFERED)

    /** One event: open the editor at this location (from the route, once). */
    val createBeaconAt: Flow<GeoUri> = _createBeaconAt.receiveAsFlow()

    /** Whether back stays on this page (closing the create menu or going up a group). */
    val handlesBack: Boolean
        get() = isCreateMenuOpen || isInGroup

    private val onLocationUpdate: () -> Boolean = { false }

    init {
        route.initialLocation?.let { _createBeaconAt.trySend(it.toGeoUri()) }
    }

    /** Closes the create menu or goes up a group; false when there is nothing left to go back from. */
    fun back(): Boolean {
        if (isCreateMenuOpen) {
            isCreateMenuOpen = false
            return true
        }
        return manager.up()
    }

    fun onResume() {
        manager.refresh()
        // Get a GPS reading
        gps.start(onLocationUpdate)
    }

    fun onPause() {
        gps.stop(onLocationUpdate)
    }

    fun search(text: String) {
        searchText = text
        manager.search(text)
    }

    fun changeSort(method: BeaconSortMethod) {
        preferences.beaconSort = method
        sort = method
        manager.refresh(true)
    }

    fun open(groupId: Long) {
        manager.open(groupId)
    }

    fun refresh() {
        manager.refresh()
    }

    override fun refreshBeaconList() {
        refresh()
    }

    fun toggleVisibility(beacon: Beacon) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                beaconService.add(beacon.copy(visible = !beacon.visible))
            }
            refresh()
        }
    }

    suspend fun setGroupVisibility(group: BeaconGroup, visible: Boolean) {
        withContext(Dispatchers.IO) {
            beaconService.loader.getChildren(group.id, null)
                .filterIsInstance<Beacon>()
                .filter { it.visible != visible }
                .forEach { beaconService.add(it.copy(visible = visible)) }
        }
        refresh()
    }

    /** Deletes [group] (every beacon when null), leaving it first if it is the one open. */
    fun delete(group: BeaconGroup?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                beaconService.delete(group)
            }
            if (group == manager.root) {
                manager.up()
            }
            refresh()
        }
    }

    /** Imports the waypoints into the open group and returns how many beacons were added. */
    suspend fun import(gpx: GPXData): Int {
        val count = withContext(Dispatchers.IO) {
            importer.import(gpx, manager.root?.id)
        }
        refresh()
        return count
    }

    suspend fun getExportGpx(): GPXData = withContext(Dispatchers.IO) {
        val all = beaconService.getBeacons(
            manager.root?.id,
            includeGroups = true,
            maxDepth = null,
            includeRoot = true
        )

        val groups = all.filterIsInstance<BeaconGroup>()
        val beacons = all.filterIsInstance<Beacon>()

        BeaconGpxConverter().toGPX(beacons, groups)
    }

    private suspend fun sortBeacons(beacons: List<IBeacon>): List<IBeacon> {
        val method = when (sort) {
            BeaconSortMethod.MostRecent -> MostRecentBeaconSort(beaconService)
            BeaconSortMethod.Closest -> ClosestBeaconSort(beaconService, gps::location)
            BeaconSortMethod.Name -> NameBeaconSort()
        }
        return method.sort(beacons)
    }

    override fun onCleared() {
        gps.stop(onLocationUpdate)
    }
}

/**
 * The beacon list: the group you're in as the page title (back goes up a group), search as the first row, every
 * group and beacon in one grouped card in the chosen sort order, and the create menu on the floating button.
 * Nothing is shown below the search bar until the first load, so "No beacons" never flashes.
 */
@Composable
private fun BeaconListScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IBeacon>,
    loaded: Boolean,
    resetKey: Int,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    currentLocation: Coordinate,
    isCreateMenuOpen: Boolean,
    onSearchChange: (String) -> Unit,
    onToolbarAction: (BeaconToolbarAction) -> Unit,
    onCreateMenuOpenChange: (Boolean) -> Unit,
    onCreateAction: (BeaconCreateAction) -> Unit,
    onBeaconAction: (Beacon, BeaconAction) -> Unit,
    onGroupAction: (BeaconGroup, BeaconGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Where are my saved places? The nearest one's distance answers it; the rest is one grouped list.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    Box(modifier = modifier.testTag("beacon_title")) {
        TrailSensePage(
            title = title,
            onBack = { dispatcher?.onBackPressed() },
            backLabel = stringResource(R.string.back),
            state = listState,
            contentPadding = PaddingValues(
                start = TrailSenseSpacing.lg,
                end = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.sm,
                bottom = 112.dp
            ),
            actions = {
                BeaconListMenu(
                    sortMenuText = sortMenuText,
                    onToolbarAction = onToolbarAction
                )
            },
            floatingActionButton = {
                // When there is nothing yet, the empty state's own button is the action; the menu returns with the
                // first place.
                if (loaded && (items.isNotEmpty() || searchText.isNotBlank())) {
                    BeaconCreateMenu(
                        isOpen = isCreateMenuOpen,
                        onOpenChange = onCreateMenuOpenChange,
                        onAction = onCreateAction
                    )
                }
            }
        ) {
            if (!loaded) {
                return@TrailSensePage
            }

            val beacons = items.filterIsInstance<Beacon>()
            val distances = beacons.associate { beacon ->
                beacon.id to Distance.meters(currentLocation.distanceTo(beacon.coordinate))
            }
            val farthest = distances.values.maxOfOrNull { it.meters().value } ?: 0f
            val nearest = beacons.minByOrNull { distances[it.id]?.meters()?.value ?: Float.MAX_VALUE }
            // With a single beacon its row already says all of this, so the answer is only shown for two or more.
            val showNearest = searchText.isBlank() && nearest != null && beacons.size > 1 &&
                    currentLocation != Coordinate.zero

            if (showNearest && nearest != null) {
                item(key = "nearest") {
                    NearestBeacon(
                        beacon = nearest,
                        currentLocation = currentLocation,
                        formatService = formatService,
                        baseDistanceUnits = baseDistanceUnits
                    )
                }
            }

            val isSearching = searchText.isNotBlank()
            // Nothing to search yet: the empty state stands alone on the page, without a search bar above it.
            if (items.isNotEmpty() || isSearching) {
                item(key = "search") {
                    TrailSenseSearchBar(
                        value = searchText,
                        onValueChange = onSearchChange,
                        placeholder = stringResource(R.string.search),
                        clearContentDescription = stringResource(R.string.beacon_search_clear),
                        modifier = Modifier.testTag("searchbox")
                    )
                }
            }

            if (items.isEmpty()) {
                item(key = "empty") {
                    TrailSenseEmptyState(
                        // A pin, not the navigation arrow: this list is where places are kept.
                        icon = if (isSearching) TrailIcons.Search else TrailIcons.Location,
                        illustration = if (isSearching) null else ({ PlacesEmptyIllustration() }),
                        title = stringResource(if (isSearching) R.string.no_results else R.string.no_places),
                        description = if (isSearching) {
                            null
                        } else {
                            stringResource(R.string.places_empty_description)
                        },
                        // The one action sits under the sentence that explains it; importing is the quiet second.
                        actionLabel = if (isSearching) null else stringResource(R.string.save_a_place),
                        actionIcon = TrailIcons.Add,
                        onAction = { onCreateAction(BeaconCreateAction.CreateBeacon) },
                        actionModifier = Modifier.testTag("create_btn"),
                        secondaryActionLabel = if (isSearching) null else stringResource(R.string.import_gpx),
                        onSecondaryAction = { onCreateAction(BeaconCreateAction.ImportGpx) },
                        tone = if (isSearching) TrailSenseTone.Secondary else TrailSenseTone.Primary,
                        modifier = Modifier
                            .fillParentMaxHeight(0.9f)
                            .testTag("beacon_empty_text")
                    )
                }
            } else {
                placesGroup(
                    key = "beacons",
                    items = items,
                    itemKey = { (if (it is BeaconGroup) "g" else "b") + it.id }
                ) { item, position, joinModifier ->
                    val rowModifier = if (position.isFirst) joinModifier.testTag("beacon_list") else joinModifier
                    when (item) {
                        is Beacon -> BeaconRow(
                            beacon = item,
                            position = position,
                            formatService = formatService,
                            baseDistanceUnits = baseDistanceUnits,
                            currentLocation = currentLocation,
                            proximity = distances[item.id]?.let { d ->
                                if (farthest <= 0f) 1f else 1f - (d.meters().value / farthest) * 0.9f
                            },
                            modifier = rowModifier,
                            onAction = { onBeaconAction(item, it) }
                        )

                        is BeaconGroup -> BeaconGroupRow(
                            group = item,
                            position = position,
                            modifier = rowModifier,
                            onAction = { onGroupAction(item, it) }
                        )
                    }
                }
            }
        }
    }
}

/** The list's answer: how far the closest beacon is, and which way. */
@Composable
private fun NearestBeacon(
    beacon: Beacon,
    currentLocation: Coordinate,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits
) {
    val bearing = remember(beacon.coordinate, currentLocation) { currentLocation.bearingTo(beacon.coordinate) }
    val distance = remember(beacon.coordinate, currentLocation, baseDistanceUnits) {
        Distance.meters(currentLocation.distanceTo(beacon.coordinate)).convertTo(baseDistanceUnits).toRelativeDistance()
    }
    val distanceText = formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    val directionText = "${formatService.formatDirection(bearing.direction)} ${
        formatService.formatDegrees(bearing.value, replace360 = true)
    }"
    // At the place itself, "0 ft · Camp is N 0° from here" says nothing; name it instead.
    if (currentLocation.distanceTo(beacon.coordinate) < 10f) {
        PlacesKeyValue(
            value = stringResource(R.string.place_you_are_here),
            verdict = beacon.name
        )
        return
    }
    // The answer as plain emphasised type; navigating and details stay on the beacon's row.
    PlacesKeyValue(
        value = distanceText,
        verdict = stringResource(R.string.places_beacon_verdict, beacon.name, directionText)
    )
}

@Composable
private fun BeaconListMenu(
    sortMenuText: String,
    onToolbarAction: (BeaconToolbarAction) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isMenuOpen = true },
            modifier = Modifier.testTag("beacon_list_menu_btn")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.beacon_more_options)
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false }
        ) {
            BeaconMenuEntry(sortMenuText, TrailIcons.SortAscending) {
                isMenuOpen = false
                onToolbarAction(BeaconToolbarAction.Sort)
            }
            BeaconMenuEntry(stringResource(R.string.export), TrailIcons.ImportExport) {
                isMenuOpen = false
                onToolbarAction(BeaconToolbarAction.Export)
            }
            BeaconMenuEntry(
                stringResource(R.string.navigate_to_nearest_cell_signal),
                TrailIcons.SignalCellular3
            ) {
                isMenuOpen = false
                onToolbarAction(BeaconToolbarAction.NavigateToNearestCellSignal)
            }
            BeaconMenuEntry(stringResource(R.string.delete), TrailIcons.Delete) {
                isMenuOpen = false
                onToolbarAction(BeaconToolbarAction.Delete)
            }
        }
    }
}

@Composable
private fun BeaconMenuEntry(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

/** The create menu: the family's expressive FAB menu, with a new beacon closest to the thumb. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeaconCreateMenu(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onAction: (BeaconCreateAction) -> Unit
) {
    val entries = listOf(
        Triple(R.string.qr_code, TrailIcons.QrCode, BeaconCreateAction.ImportQr),
        Triple(R.string.import_gpx, TrailIcons.ImportExport, BeaconCreateAction.ImportGpx),
        Triple(R.string.group, TrailIcons.BeaconGroup, BeaconCreateAction.CreateGroup),
        Triple(R.string.beacon, TrailIcons.Location, BeaconCreateAction.CreateBeacon)
    )
    FloatingActionButtonMenu(
        expanded = isOpen,
        button = {
            ToggleFloatingActionButton(
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    initialColor = MaterialTheme.colorScheme.primary,
                    finalColor = MaterialTheme.colorScheme.primary
                ),
                checked = isOpen,
                onCheckedChange = { onOpenChange(it) },
                modifier = Modifier.testTag("create_btn")
            ) {
                val progress = checkedProgress
                Icon(
                    tint = MaterialTheme.colorScheme.onPrimary,
                    imageVector = if (progress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = stringResource(if (isOpen) R.string.beacon_create_menu_close else R.string.add),
                    modifier = with(ToggleFloatingActionButtonDefaults) {
                        Modifier.animateIcon(
                            { progress },
                            color = ToggleFloatingActionButtonDefaults.iconColor(
                                initialColor = MaterialTheme.colorScheme.onPrimary,
                                finalColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                )
            }
        }
    ) {
        entries.forEach { (label, icon, action) ->
            FloatingActionButtonMenuItem(
                onClick = { onAction(action) },
                icon = { Icon(icon, contentDescription = null) },
                text = { Text(stringResource(label)) }
            )
        }
    }
}

@Composable
private fun BeaconRow(
    beacon: Beacon,
    position: TrailSenseGroupPosition,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    currentLocation: Coordinate,
    proximity: Float?,
    modifier: Modifier = Modifier,
    onAction: (BeaconAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val distance = remember(beacon.coordinate, currentLocation, baseDistanceUnits) {
        val meters = NavigationService().navigate(beacon.coordinate, currentLocation, 0f).distance
        Distance.meters(meters).convertTo(baseDistanceUnits).toRelativeDistance()
    }
    val bearing = remember(beacon.coordinate, currentLocation) { currentLocation.bearingTo(beacon.coordinate) }
    val hasLocation = currentLocation != Coordinate.zero
    val isHere = hasLocation && currentLocation.distanceTo(beacon.coordinate) < 10f
    val badgeColor = Color(getBeaconBadgeColor(beacon, formatService))
    val badgeContent = if (badgeColor.luminance() > 0.5f) Color.Black else Color.White
    val subtitle = listOfNotNull(
        // "SE 131° · 2.87 km": which way and how far share the line under the name, so the name is the row's one
        // focal point and the trailing edge only holds the row's controls.
        // At the place, "N 0° · 0 ft" becomes "You're here".
        if (isHere) {
            stringResource(R.string.place_you_are_here)
        } else if (hasLocation) {
            "${formatService.formatDirection(bearing.direction)} ${
                formatService.formatDegrees(bearing.value, replace360 = true)
            }"
        } else {
            null
        },
        if (isHere) null else formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units), false),
        beacon.elevation?.let {
            formatService.formatElevation(Distance.meters(it).convertTo(baseDistanceUnits))
        },
        if (!beacon.visible && !beacon.temporary) stringResource(R.string.places_hidden) else null
    ).joinToString(" · ").ifBlank { null }

    PlacesRow(
        title = beacon.name,
        subtitle = subtitle,
        position = position,
        modifier = modifier,
        dimmed = !beacon.visible && !beacon.temporary,
        leading = {
            PlacesBadge(
                containerColor = badgeColor,
                contentColor = badgeContent,
                icon = getBeaconIcon(beacon, formatService),
                shape = if (beacon.owner == BeaconOwner.User) {
                    beacon.shape.polygon().toShape()
                } else {
                    MaterialShapes.Pentagon.toShape()
                },
                size = 40.dp
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        barFraction = null,
        onClick = { onAction(BeaconAction.View) },
        onLongClick = { onAction(BeaconAction.Navigate) },
        actions = {
            BeaconRowActions(
                beacon = beacon,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        }
    )
}

@Composable
private fun BeaconRowActions(
    beacon: Beacon,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconAction) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!beacon.temporary) {
            IconButton(
                onClick = { onAction(BeaconAction.ToggleVisibility) },
                modifier = Modifier.testTag("beacon_visibility_toggle")
            ) {
                Icon(
                    painter = rememberVectorPainter(
                        if (beacon.visible) {
                            TrailIcons.Visible
                        } else {
                            TrailIcons.NotVisible
                        }
                    ),
                    contentDescription = stringResource(
                        if (beacon.visible) R.string.hide else R.string.show
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(
                onClick = { onMenuOpenChange(true) },
                modifier = Modifier.testTag("beacon_item_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.beacon_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) }
            ) {
                BeaconMenuItem(R.string.navigate, onMenuOpenChange, onAction, BeaconAction.Navigate)
                BeaconMenuItem(R.string.share_ellipsis, onMenuOpenChange, onAction, BeaconAction.Share)
                if (!beacon.temporary) {
                    BeaconMenuItem(R.string.edit, onMenuOpenChange, onAction, BeaconAction.Edit)
                    BeaconMenuItem(R.string.move_to, onMenuOpenChange, onAction, BeaconAction.Move)
                    BeaconMenuItem(R.string.delete, onMenuOpenChange, onAction, BeaconAction.Delete)
                }
            }
        }
    }
}

@Composable
private fun BeaconMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconAction) -> Unit,
    action: BeaconAction
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
private fun BeaconGroupRow(
    group: BeaconGroup,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onAction: (BeaconGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    PlacesRow(
        title = group.name,
        subtitle = pluralStringResource(R.plurals.beacon_group_summary, group.count, group.count),
        position = position,
        modifier = modifier,
        leading = {
            // The count is in the subtitle, so the mark is the group's icon rather than the number again.
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(TrailIcons.BeaconGroup))
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = { onAction(BeaconGroupAction.Open) },
        // One trailing element: the row itself opens the group, so no chevron beside the menu.
        actions = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("beacon_group_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.beacon_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    BeaconGroupMenuItem(R.string.show_all, { menuOpen = it }, onAction, BeaconGroupAction.ShowAll)
                    BeaconGroupMenuItem(R.string.hide_all, { menuOpen = it }, onAction, BeaconGroupAction.HideAll)
                    BeaconGroupMenuItem(R.string.rename, { menuOpen = it }, onAction, BeaconGroupAction.Edit)
                    BeaconGroupMenuItem(R.string.move_to, { menuOpen = it }, onAction, BeaconGroupAction.Move)
                    BeaconGroupMenuItem(R.string.delete, { menuOpen = it }, onAction, BeaconGroupAction.Delete)
                }
            }
        }
    )
}

@Composable
private fun BeaconGroupMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconGroupAction) -> Unit,
    action: BeaconGroupAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}


private fun getBeaconIcon(beacon: Beacon, formatService: FormatService): ImageVector {
    if (beacon.owner == BeaconOwner.User) {
        return beacon.icon?.icon ?: TrailIcons.Location
    }

    return CellSignalUtils.getCellQualityImage(getCellSignalQuality(beacon, formatService))
}

private fun getBeaconBadgeColor(beacon: Beacon, formatService: FormatService): Int {
    if (beacon.owner == BeaconOwner.User) {
        return beacon.color
    }
    return CustomUiUtils.getQualityColor(getCellSignalQuality(beacon, formatService))
}

private fun getCellSignalQuality(beacon: Beacon, formatService: FormatService): Quality {
    return when {
        beacon.name.contains(formatService.formatQuality(Quality.Poor)) -> Quality.Poor
        beacon.name.contains(formatService.formatQuality(Quality.Moderate)) -> Quality.Moderate
        beacon.name.contains(formatService.formatQuality(Quality.Good)) -> Quality.Good
        else -> Quality.Unknown
    }
}

private enum class BeaconToolbarAction {
    Sort,
    Export,
    NavigateToNearestCellSignal,
    Delete
}

private enum class BeaconCreateAction {
    ImportQr,
    ImportGpx,
    CreateGroup,
    CreateBeacon
}

private val QR_IMPORT_CAMERA_SIZE = Size(200, 200)

@Preview(showBackground = true)
@Composable
private fun BeaconListScreenPreview() {
    TrailSenseTheme {
        BeaconListScreen(
            title = "Beacons",
            sortMenuText = "Sort by Closest",
            searchText = "",
            items = emptyList(),
            loaded = true,
            resetKey = 0,
            formatService = FormatService.getInstance(androidx.compose.ui.platform.LocalContext.current),
            baseDistanceUnits = DistanceUnits.Meters,
            currentLocation = Coordinate.zero,
            isCreateMenuOpen = false,
            onSearchChange = {},
            onToolbarAction = {},
            onCreateMenuOpenChange = {},
            onCreateAction = {},
            onBeaconAction = { _, _ -> },
            onGroupAction = { _, _ -> }
        )
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
