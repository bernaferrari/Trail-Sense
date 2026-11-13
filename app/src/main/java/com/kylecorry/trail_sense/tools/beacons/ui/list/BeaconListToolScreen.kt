@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui.list

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.luna.extensions.filterIndices
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.item
import com.kylecorry.trail_sense.shared.dialogs.items
import com.kylecorry.trail_sense.shared.dialogs.snackbar
import com.kylecorry.trail_sense.shared.dialogs.withLoading
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.navigation.toGeoUri
import com.kylecorry.trail_sense.shared.navigation.toRouteLocation
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.PlacesEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.domain.icon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.CreateBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.RenameBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell.NavigateToNearestCellSignal
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.MostRecentBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
import com.kylecorry.trail_sense.tools.beacons.ui.polygon
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.ui.QRImportContent
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            beaconService = getAppService<IBeaconService>(),
            preferences = UserPreferences(appContext).beacons,
            gps = getAppService<SensorService>().getGPS(),
            importer = BeaconGpxImporter()
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
            val exportFile = "trail-sense-${Clock.System.now().epochSeconds}.gpx"
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
                        Alerts.snackbar(
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
                CreateBeaconGroupCommand(scope, viewModel.beaconService) {
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
            BeaconGroupAction.Edit -> RenameBeaconGroupCommand(scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(group)

            BeaconGroupAction.Delete -> delete(group)
            BeaconGroupAction.Move -> MoveBeaconGroupCommand(scope, viewModel.beaconService) {
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

            BeaconAction.Delete -> DeleteBeaconCommand(scope, viewModel.beaconService) {
                viewModel.refresh()
            }.execute(beacon)

            BeaconAction.Move -> MoveBeaconCommand(scope, viewModel.beaconService) {
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
    SystemBackHandler(enabled = viewModel.handlesBack) {
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
                    camera.start(QR_IMPORT_CAMERA_SIZE) { bitmap ->
                        onQrImportCameraUpdate(bitmap)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val QR_IMPORT_CAMERA_SIZE = Size(200, 200)

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
