package com.kylecorry.trail_sense.tools.offline_maps.ui.trail_maps

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.LayerFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
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

class ViewVectorMapFragment : TrailSenseComposeFragment() {

    private val service by lazy { getAppService<MapService>() }
    private var map by mutableStateOf<VectorMap?>(null)
    private var mapView: MapView? = null
    private var configuredMapId: Long? = null

    private val mapId: Long
        get() = requireArguments().getLong("offline_map_file_id")

    @Composable
    override fun FragmentContent() {
        VectorMapScreen(
            map = map,
            onMapReady = ::onMapReady,
            onZoomIn = { mapView?.zoom(2f) },
            onZoomOut = { mapView?.zoom(0.5f) },
            onRename = { map?.let { rename(it) } },
            onEditAttribution = { map?.let { editAttribution(it) } },
            onDelete = { map?.let { delete(it) } }
        )
    }

    override fun onResume() {
        super.onResume()
        loadMap()
    }

    override fun onPause() {
        mapView?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView?.stop()
        mapView = null
        configuredMapId = null
        super.onDestroyView()
    }

    private fun onMapReady(view: MapView) {
        mapView = view
        configureMap(view, map)
    }

    private fun loadMap() {
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                service.getVectorMap(mapId)
            }
            map = loaded
            configuredMapId = null
            mapView?.let { configureMap(it, loaded) }
        }
    }

    private fun configureMap(view: MapView, map: VectorMap?) {
        if (configuredMapId == map?.id) {
            return
        }

        view.stop()
        configuredMapId = map?.id

        if (map == null) {
            view.setLayers(emptyList())
            return
        }

        val factory = LayerFactory()
        val layer = Tools.getMapLayerDefinition(requireContext(), TrailMapsTileSource.SOURCE_ID)!!
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

    private fun rename(map: VectorMap) {
        viewLifecycleOwner.lifecycleScope.launch {
            RenameMapCommand(requireContext(), service).execute(map)
            loadMap()
        }
    }

    private fun editAttribution(map: VectorMap) {
        viewLifecycleOwner.lifecycleScope.launch {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            loadMap()
        }
    }

    private fun delete(map: VectorMap) {
        viewLifecycleOwner.lifecycleScope.launch {
            DeleteMapCommand(requireContext(), service).execute(map)
            if (withContext(Dispatchers.IO) { service.getVectorMap(map.id) } == null) {
                onMain {
                    findNavController().popBackStack()
                }
            } else {
                loadMap()
            }
        }
    }
}

@Composable
private fun VectorMapScreen(
    map: VectorMap?,
    onMapReady: (MapView) -> Unit,
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = map?.name ?: stringResource(R.string.offline_maps),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 64.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu_dots),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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
            }

            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            keepScreenOn = true
                            backgroundColorOverride = AndroidColor.rgb(127, 127, 127)
                            onMapReady(this)
                        }
                    },
                    update = {
                        onMapReady(it)
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("map")
                )

                SmallFloatingActionButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp)
                        .size(40.dp)
                        .testTag("zoom_in_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null
                    )
                }

                SmallFloatingActionButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 32.dp)
                        .size(40.dp)
                        .testTag("zoom_out_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_zoom_out),
                        contentDescription = null
                    )
                }
            }
        }
    }
}
