@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.LayerFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.TrailMapsTileSource
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ViewVectorMapToolScreen(route: Route.VectorMapView) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel { ViewVectorMapViewModel(getAppService(), route.offlineMapFileId) }
    val mapView = remember {
        MapView(context).apply {
            keepScreenOn = true
            backgroundColorOverride = AndroidColor.rgb(127, 127, 127)
        }
    }

    // Each load (including after a rename or attribution edit) shows the map's tiles and fits its bounds
    LaunchedEffect(mapView, viewModel.loadCount) {
        if (viewModel.loadCount > 0) {
            configureMap(context, mapView, viewModel.map)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        mapView.stop()
    }

    DisposableEffect(mapView) {
        onDispose { mapView.stop() }
    }

    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { navigator.popBackStack() }
    }

    VectorMapScreen(
        map = viewModel.map,
        mapView = mapView,
        onZoomIn = { mapView.zoom(2f) },
        onZoomOut = { mapView.zoom(0.5f) },
        onRename = {
            viewModel.map?.let {
                scope.launch {
                    RenameMapCommand(context, getAppService()).execute(it)
                    viewModel.load()
                }
            }
        },
        onEditAttribution = {
            viewModel.map?.let {
                scope.launch {
                    EditOfflineMapAttributionCommand(context).execute(it)
                    viewModel.load()
                }
            }
        },
        onDelete = {
            viewModel.map?.let {
                scope.launch {
                    DeleteMapCommand(context, getAppService()).execute(it)
                    viewModel.onDeleteAttempted(it)
                }
            }
        }
    )
}

private fun configureMap(context: Context, view: MapView, map: VectorMap?) {
    view.stop()

    if (map == null) {
        view.setLayers(emptyList())
        return
    }

    val factory = LayerFactory()
    val layer = Tools.getMapLayerDefinition(context, TrailMapsTileSource.SOURCE_ID)!!
    val tileLayer = factory.createLayer(layer)
    tileLayer.setFeatureFilter(map.id.toString())
    view.setLayers(listOf(tileLayer))
    view.start()
    map.bounds?.let { bounds ->
        view.doOnLayout {
            view.fitIntoView(bounds)
        }
    }
}

internal class ViewVectorMapViewModel(
    private val service: MapService,
    private val mapId: Long
) : ViewModel() {
    var map by mutableStateOf<VectorMap?>(null)
        private set

    /** Counts finished loads, so the map is set up again after each one. */
    var loadCount by mutableIntStateOf(0)
        private set

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            map = withContext(Dispatchers.IO) {
                service.getVectorMap(mapId)
            }
            loadCount++
        }
    }

    /** After the delete prompt: leave if the map is gone, otherwise show it again. */
    fun onDeleteAttempted(map: VectorMap) {
        viewModelScope.launch {
            if (withContext(Dispatchers.IO) { service.getVectorMap(map.id) } == null) {
                _deleted.send(Unit)
            } else {
                load()
            }
        }
    }
}

@Composable
private fun VectorMapScreen(
    map: VectorMap?,
    mapView: MapView,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRename: () -> Unit,
    onEditAttribution: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentTopBar(
                title = map?.name ?: stringResource(R.string.offline_maps),
                modifier = Modifier.testTag("title"),
                actions = {
                    Box {
                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(
                                imageVector = TrailIcons.MenuDots,
                                contentDescription = stringResource(R.string.map_options)
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    isMenuOpen = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.attribution)) },
                                onClick = {
                                    isMenuOpen = false
                                    onEditAttribution()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    isMenuOpen = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                mapView.Content(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("map")
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = TrailSenseSpacing.screen, bottom = TrailSenseSpacing.xxl),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = TrailSenseSpacing.xxs,
                    shadowElevation = TrailSenseSpacing.xs
                ) {
                    Column(
                        modifier = Modifier.padding(TrailSenseSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                    ) {
                        FilledIconButton(
                            onClick = onZoomIn,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("zoom_in_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = TrailIcons.Add,
                                contentDescription = stringResource(R.string.zoom_in)
                            )
                        }

                        FilledIconButton(
                            onClick = onZoomOut,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("zoom_out_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = TrailIcons.ZoomOut,
                                contentDescription = stringResource(R.string.zoom_out)
                            )
                        }
                    }
                }
            }
        }
    }
}
