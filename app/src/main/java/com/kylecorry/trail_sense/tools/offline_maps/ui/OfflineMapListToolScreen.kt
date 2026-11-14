@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.offline_maps.ui

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.item
import com.kylecorry.trail_sense.shared.dialogs.loading
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.OfflineMapsEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
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
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateBlankMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromCameraCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromFileCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromUriCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.ICreateMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapPreferences
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.MapCleanupCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.hasPdf
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.CreateMapGroupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.MoveMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ResizeMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ShowMapsDisclaimerCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ToggleVisibilityMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapGroupAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.VectorMapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapExportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun OfflineMapListToolScreen(
    route: Route.OfflineMapList,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    val gps = remember { getAppService<SensorService>().getGPS() }
    val viewModel = viewModel {
        val appContext = context.applicationContext
        val appGps = getAppService<SensorService>().getGPS()
        OfflineMapListViewModel(
            getAppService<MapService>(),
            UserPreferences(appContext).photoMaps,
            locationProvider = { appGps.location }
        )
    }
    // The file picker needs the activity's result launcher (an Activity-only API).
    val intentResultRetriever = remember(context) { requireNotNull(context.findMainActivity()) }
    val uriPicker = remember(context) { IntentUriPicker(intentResultRetriever, context) }
    val mapImportingIndicator = remember(context) {
        AlertLoadingIndicator(context.getString(R.string.importing_map))
    }
    val exportService = remember(context, effects, lifecycleOwner) {
        PhotoMapExportService(context, intentResultRetriever, effects, lifecycleOwner)
    }
    var isCreateMenuOpen by remember { mutableStateOf(false) }

    // Back leaves the open group first, then the screen.
    SystemBackHandler {
        if (!viewModel.up()) {
            navigator.navigateUp()
        }
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume(cleanup = { MapCleanupCommand(context.applicationContext).execute() })
        onPauseOrDispose { }
    }

    fun createMap(command: ICreateMapCommand, onDone: () -> Unit = {}) {
        scope.launch {
            MapCleanupCommand.isMapImportInProgress = true
            try {
                viewModel.isCreateEnabled = false

                val result = command.execute()
                val map = when (val created = result.mapOrNull()) {
                    is PhotoMap -> created.copy(parentId = viewModel.rootId)
                    is VectorMap -> created.copy(parentId = viewModel.rootId)
                    else -> null
                }

                if (map == null) {
                    val error = (result as? CreateMapResult.Failed)?.error
                        ?: CreateOfflineMapError.InvalidMapFile
                    // Cancelling isn't an error, so it gets no message
                    createMapErrorMessage(error)?.let {
                        effects.showMessage(context.getString(it))
                    }
                    return@launch
                }

                if (map.parentId != null) {
                    viewModel.save(map)
                }

                if (map is PhotoMap) {
                    val isPdfMap = map.hasPdf()
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

                viewModel.refresh(true)
                onDone()
                when (map) {
                    is PhotoMap -> navigator.navigate(Route.PhotoMaps(mapId = map.id))
                }
            } finally {
                MapCleanupCommand.isMapImportInProgress = false
                viewModel.isCreateEnabled = true
            }
        }
    }

    // The safety disclaimer is shown on the first visit, empty list or not, until it is closed once
    LaunchedEffect(Unit) {
        ShowMapsDisclaimerCommand(AppHost.from(context), navigator).execute()
    }

    // A map shared to the app is imported once
    LaunchedEffect(viewModel) {
        val mapIntentUri = route.importUri?.toUri()
        if (mapIntentUri != null && viewModel.claimImport()) {
            createMap(CreateMapFromUriCommand(context, mapIntentUri, mapImportingIndicator)) {
                // Without the URI, coming back to this screen doesn't import it again.
                navigator.replace(Route.OfflineMapList())
            }
        }
    }

    /** Runs a command that asks the user something, then shows the list again. */
    fun runAndRefresh(block: suspend () -> Unit) {
        scope.launch {
            block()
            viewModel.refresh()
        }
    }

    fun delete(map: IMap) = runAndRefresh { DeleteMapCommand(viewModel.mapService, AppHost.from(context), getAppService()).execute(map) }
    fun rename(map: IMap) = runAndRefresh { RenameMapCommand(viewModel.mapService, AppHost.from(context), getAppService()).execute(map) }
    fun move(map: IMap) = runAndRefresh { MoveMapCommand(AppHost.from(context), viewModel.mapService).execute(map) }

    fun view(map: IMap) {
        when (map) {
            is MapGroup -> viewModel.open(map)
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

    OfflineMapListScreen(
        title = viewModel.rootName ?: stringResource(R.string.offline_maps),
        sortMenuText = stringResource(R.string.sort_by, viewModel.sort.label()),
        searchText = viewModel.searchText,
        items = viewModel.items,
        // A search with no matches still shows the search box, so it can be cleared
        isEmpty = viewModel.items.isEmpty() && viewModel.searchText.isBlank(),
        resetKey = viewModel.resetKey,
        isCreateMenuOpen = isCreateMenuOpen,
        isCreateEnabled = viewModel.isCreateEnabled,
        canPrint = remember { Printer.canPrint() },
        gpsLocationOnMap = { map ->
            when (map) {
                is PhotoMap -> map.boundary()?.contains(gps.location) ?: false
                is VectorMap -> map.bounds?.contains(gps.location) ?: false
                else -> false
            }
        },
        onSearchChange = viewModel::search,
        onToolbarAction = { action ->
            when (action) {
                OfflineMapToolbarAction.Sort -> {
                    val sortOptions = MapSortMethod.entries
                    Pickers.item(
                        context,
                        context.getString(R.string.sort),
                        sortOptions.map { it.label() },
                        sortOptions.indexOf(viewModel.sort)
                    ) { newSort ->
                        if (newSort != null) {
                            viewModel.changeSort(sortOptions[newSort])
                        }
                    }
                }
            }
        },
        onCreateMenuOpenChange = { isCreateMenuOpen = it },
        onCreateAction = { action ->
            when (action) {
                OfflineMapCreateAction.CreateGroup -> runAndRefresh {
                    CreateMapGroupCommand(viewModel.mapService).execute(viewModel.rootId)
                }

                OfflineMapCreateAction.ImportFile -> createMap(
                    CreateMapFromFileCommand(context, uriPicker, mapImportingIndicator)
                )

                OfflineMapCreateAction.ImportCamera -> createMap(
                    CreateMapFromCameraCommand(context, effects, mapImportingIndicator)
                )

                OfflineMapCreateAction.CreateBlank -> createMap(
                    CreateBlankMapCommand(context, mapImportingIndicator)
                )
            }
            isCreateMenuOpen = false
        },
        onPhotoMapAction = { map, action ->
            when (action) {
                MapAction.View -> view(map)
                MapAction.Delete -> delete(map)
                MapAction.Export -> exportService.export(map)
                MapAction.Print -> scope.launch { PrintMapCommand(context).execute(map) }
                MapAction.Resize -> runAndRefresh { ResizeMapCommand(context, mapImportingIndicator).execute(map) }
                MapAction.Rename -> rename(map)
                MapAction.Move -> move(map)
                MapAction.ToggleVisibility -> viewModel.toggleVisibility(map)
            }
        },
        onVectorMapAction = { map, action ->
            when (action) {
                VectorMapAction.View -> view(map)
                VectorMapAction.Rename -> rename(map)
                VectorMapAction.EditAttribution -> runAndRefresh {
                    EditOfflineMapAttributionCommand().execute(map)
                }

                VectorMapAction.Export -> exportService.export(map)
                VectorMapAction.Delete -> delete(map)
                VectorMapAction.Move -> move(map)
                VectorMapAction.ToggleVisibility -> viewModel.toggleVisibility(map)
                VectorMapAction.CopyToAppStorage -> scope.launch {
                    mapImportingIndicator.show()
                    try {
                        viewModel.copyToAppStorage(map)
                    } finally {
                        mapImportingIndicator.hide()
                    }
                }
            }
        },
        onMapGroupAction = { group, action ->
            when (action) {
                MapGroupAction.View -> view(group)
                MapGroupAction.Delete -> delete(group)
                MapGroupAction.Rename -> rename(group)
                MapGroupAction.Move -> move(group)
                MapGroupAction.ShowAll -> setGroupVisibility(AppHost.from(context), scope, viewModel, group, true)
                MapGroupAction.HideAll -> setGroupVisibility(AppHost.from(context), scope, viewModel, group, false)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}
