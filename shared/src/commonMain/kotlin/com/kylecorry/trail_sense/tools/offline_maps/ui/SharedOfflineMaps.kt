@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineVectorMap
import kotlinx.coroutines.flow.MutableStateFlow
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.views.compose.*
import com.kylecorry.trail_sense.tools.map.ui.SharedMapView
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceLayer
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun SharedOfflineMapList() {
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val strings = remember { getAppService<AppStrings>() }
    val maps = remember { getAppService<MapService>() }
    val files = remember { getAppService<SharedMapFiles>() }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<IMap>?>(null) }
    var busy by remember { mutableStateOf(false) }
    suspend fun load() {
        items = (maps.getAllPhotoMaps() + maps.getAllVectorMaps()).sortedBy { it.name.lowercase() }
    }
    LaunchedEffect(maps) { load() }

    fun importMap(action: suspend () -> IMap?) {
        busy = true
        scope.launch {
            try {
                val map = action() ?: return@launch
                load()
                navigator.navigate(map.destination())
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                effects.showMessage(strings.get(Res.string.shared_map_invalid))
            } finally {
                busy = false
            }
        }
    }

    TrailSensePage(
        title = appString(Res.string.offline_maps),
        onBack = rememberTrailSenseBack(),
        actions = {
            TextButton(enabled = !busy, onClick = {
                importMap {
                    val file = FileKit.openFilePicker(type = FileKitType.File(files.extensions.toSet())) ?: return@importMap null
                    if (file.extension.equals("pdf", ignoreCase = true)) {
                        val document = files.preparePdf(file)
                        val page = chooseOfflinePdfPage(AppHost.current(), document.pageCount) ?: return@importMap null
                        files.importPdf(document, page, maps)
                    } else {
                        files.import(file, maps)
                    }
                }
            }) { Text(appString(Res.string.import_btn)) }
        }
    ) {
        val current = items
        when {
            current == null || busy -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            current.isEmpty() -> item {
                Text(appString(Res.string.shared_map_empty))
            }
        }
        items(current.orEmpty(), key = { "${it is PhotoMap}-${it.id}" }) { map ->
            TrailSenseListItem(
                title = map.name,
                subtitle = when (map) {
                    is VectorMap -> if (map.isAvailable) null else appString(Res.string.shared_map_unavailable)
                    else -> null
                },
                modifier = Modifier.padding(bottom = 4.dp),
                onClick = { navigator.navigate(map.destination()) }
            )
        }
    }
}

private fun IMap.destination(): Route = when (this) {
    is PhotoMap -> Route.PhotoMaps(id)
    else -> Route.VectorMapView(id)
}

private class OfflineVectorSource : GeoJsonSource {
    val maps = MutableStateFlow(emptyList<OfflineVectorMap>())
    override suspend fun load(bounds: CoordinateBounds, zoom: Int, params: MapLayerValues): GeoJsonFeatureCollection {
        return GeoJsonFeatureCollection(maps.value.flatMap { it.features(bounds, zoom) })
    }
}

private class OfflineVectorLayer(private val files: OfflineVectorSource = OfflineVectorSource()) :
    GeoJsonLayer<OfflineVectorSource>(files, "offline", useFastPathRendering = true) {
    fun setMaps(maps: List<OfflineVectorMap>) {
        files.maps.value = maps
        refresh()
    }
}

@Composable
internal fun SharedOfflineMap(
    mapId: Long? = null,
    mapFiles: SharedMapFiles? = null,
    drawPath: Boolean = false,
    pathGroupId: Long? = null
) {
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val strings = remember { getAppService<AppStrings>() }
    val maps = remember { getAppService<MapService>() }
    val files = mapFiles ?: remember { getAppService<SharedMapFiles>() }
    val paths = remember { getAppService<IPathService>() }
    val prefs = remember { getAppService<UserPreferences>().paths }
    val sensors = remember { getAppService<ISensorSubsystem>() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = remember(density, mapId) { SharedMapView(density.density, density.fontScale) }
    val offline = remember(view) { OfflineVectorLayer() }
    val raster = remember(view) { OfflineRasterLayer() }
    val track = remember(view) { ConfigurableGeoJsonLayer(true, layerId = "paths") }
    val position = remember(view) { ConfigurableGeoJsonLayer(false, layerId = "position") }
    val editor = remember(view, drawPath) { MapDistanceLayer().takeIf { drawPath } }
    var points by remember(editor) { mutableStateOf(emptyList<Coordinate>()) }
    var editState by remember(editor) { mutableStateOf(editor?.editState) }
    var savingPath by remember(editor) { mutableStateOf(false) }
    var confirmDiscard by remember(editor) { mutableStateOf(false) }
    val positionColor = MaterialTheme.colorScheme.primary.toArgb()
    val trackColor = MaterialTheme.colorScheme.tertiary.toArgb()
    var mapName by remember(mapId) { mutableStateOf<String?>(null) }
    var attribution by remember(mapId) { mutableStateOf<String?>(null) }
    var canDelete by remember(mapId) { mutableStateOf(false) }
    var confirmDelete by remember(mapId) { mutableStateOf(false) }
    var unavailable by remember(mapId) { mutableStateOf(false) }
    var located by remember(view) { mutableStateOf(false) }

    SystemBackHandler(enabled = savingPath || points.isNotEmpty()) {
        if (!savingPath) confirmDiscard = true
    }

    DisposableEffect(view, editor) {
        editor?.onPathChanged = { points = it }
        editor?.onEditStateChanged = { editState = it }
        view.layerManager.setLayers(listOfNotNull(offline, raster, track, position, editor))
        view.layerManager.start()
        onDispose { view.layerManager.dispose() }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(appString(Res.string.unsaved_changes)) },
            text = { Text(appString(Res.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = { navigator.navigateUp() }) { Text(appString(Res.string.dialog_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(systemString(SystemString.Cancel)) }
            }
        )
    }

    LaunchedEffect(view) {
        val selected = mapId?.let { maps.getVectorMap(it) }
        mapName = selected?.name
        canDelete = selected != null
        val displayed = if (mapId == null) maps.getAllVectorMaps().filter { it.visible } else listOfNotNull(selected)
        val sources = displayed.mapNotNull { map ->
            try {
                files.openVector(map)?.let { map to it }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        }
        unavailable = mapId != null && sources.isEmpty()
        attribution = sources.mapNotNull { it.first.attribution }.distinct().joinToString(" · ").ifBlank { null }
        offline.setMaps(sources.map { it.second })
        displayed.firstNotNullOfOrNull { it.bounds }?.let { bounds ->
            view.doOnLayout { view.fitIntoView(bounds) }
        }
    }

    LaunchedEffect(view, files) {
        if (mapId != null) return@LaunchedEffect
        val candidates = maps.getAllPhotoMaps().mapNotNull { photo -> rasterPreviewBounds(photo)?.let { photo to it } }
        if (maps.getAllVectorMaps().none { it.visible && it.bounds != null }) {
            candidates.firstOrNull()?.second?.let { bounds -> view.doOnLayout { view.fitIntoView(bounds) } }
        }
        snapshotFlow { view.mapBounds }
            .map { bounds ->
                candidates.filter { (_, footprint) -> footprint.intersects(bounds) }
                    .sortedBy { (_, footprint) -> footprint.center.distanceTo(bounds.center) }
                    .take(3)
                    .map { (photo, _) -> photo }
            }
            .distinctUntilChanged()
            .collectLatest { visible ->
                val loaded = raster.maps.associateBy { it.map.id }
                raster.maps = visible.mapNotNull { photo ->
                    loaded[photo.id] ?: try {
                        files.openPhoto(photo)?.takeIf { (map, _) -> map.isCalibrated }
                            ?.let { (map, image) -> RasterMapOverlay(map, image) }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        null
                    }
                }
                view.invalidate()
            }
    }

    LaunchedEffect(view, trackColor) {
        paths.getPaths().collectLatest { allPaths ->
            val backtrackId = paths.getBacktrackPathId()
            val visible = allPaths.filter { it.style.visible || it.id == backtrackId }
            val points = if (visible.isEmpty()) emptyMap() else paths.getWaypoints(visible.map { it.id })
            track.setData(GeoJsonFeatureCollection(visible.mapNotNull { path ->
                val coordinates = points[path.id].orEmpty().sortedBy { it.id }.map { it.coordinate }
                if (coordinates.size < 2) null else GeoJsonFeature.lineString(
                    coordinates, path.id, path.name, lineStyle = path.style.line,
                    color = if (path.id == backtrackId) trackColor else path.style.color
                )
            }))
        }
    }

    fun centerOn(coordinate: Coordinate) {
        view.doOnLayout {
            view.fitIntoView(
                CoordinateBounds(
                    (coordinate.latitude + .005).coerceAtMost(90.0),
                    Coordinate.toLongitude(coordinate.longitude + .005),
                    (coordinate.latitude - .005).coerceAtLeast(-90.0),
                    Coordinate.toLongitude(coordinate.longitude - .005)
                )
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(appString(Res.string.delete)) },
            text = { Text(mapName.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        mapId?.let { maps.getVectorMap(it) }?.let { maps.delete(it) }
                        navigator.navigateUp()
                    }
                }) { Text(appString(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(appString(Res.string.back)) }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        val back = rememberTrailSenseBack()
        TopAppBar(
            title = { Text(if (drawPath) appString(Res.string.create_path_on_map) else mapName ?: appString(Res.string.map)) },
            navigationIcon = {
                if (back != null) TextButton(enabled = !savingPath, onClick = {
                    if (points.isNotEmpty()) confirmDiscard = true else back()
                }) { Text(appString(Res.string.back)) }
            },
            actions = {
                if (mapId == null && !drawPath) {
                    TextButton(onClick = { navigator.navigate(Route.OfflineMapList()) }) { Text(appString(Res.string.maps)) }
                } else if (canDelete) {
                    TextButton(onClick = { confirmDelete = true }) { Text(appString(Res.string.delete)) }
                }
                TextButton(enabled = !located, onClick = {
                    located = true
                    scope.launch {
                        try {
                            val coordinate = sensors.getLocation()
                            position.setData(GeoJsonFeature.point(coordinate, color = positionColor))
                            centerOn(coordinate)
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            effects.showMessage(strings.get(Res.string.shared_map_location_unavailable))
                        } finally {
                            located = false
                        }
                    }
                }) { Text(appString(Res.string.my_location)) }
            }
        )
        if (unavailable) Text(appString(Res.string.shared_map_unavailable), Modifier.padding(16.dp))
        view.Content(Modifier.weight(1f).fillMaxWidth())
        if (editor != null) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(LocalScreenBottomInsets.current)) {
                TextButton(enabled = !savingPath && editState?.canUndo == true, onClick = editor::undo) {
                    Text(appString(Res.string.undo))
                }
                TextButton(enabled = !savingPath && editState?.canRedo == true, onClick = editor::redo) {
                    Text(appString(Res.string.redo))
                }
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !savingPath && points.size >= 2, onClick = {
                    val coordinates = points
                    savingPath = true
                    view.isInteractive = false
                    scope.launch {
                        try {
                            val host = AppHost.current()
                            val name = CoroutinePickers.text(host, strings.get(Res.string.path), hint = strings.get(Res.string.name))
                                ?: return@launch
                            val id = Alerts.withLoading(host, strings.get(Res.string.loading)) {
                                CreatePathCommand(paths, prefs, name.trim().ifBlank { null }, parentId = pathGroupId)
                                    .execute(coordinates)
                            }
                            navigator.replace(Route.PathOverview(id))
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            effects.showMessage(strings.get(Res.string.unavailable))
                        } finally {
                            savingPath = false
                            view.isInteractive = true
                        }
                    }
                }) { Text(appString(Res.string.save)) }
            }
        }
        attribution?.let {
            Text(
                it,
                Modifier.windowInsetsPadding(LocalScreenBottomInsets.current).padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
