package com.kylecorry.trail_sense.tools.offline_maps.ui.trail_maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

@Composable
fun Fragment.VectorMapContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_offline_map_view, null)
            },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            "Map controls will be pure Compose",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

class ViewVectorMapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        VectorMapContent()
    }
}
        val (refreshKey, refresh) = useTrigger()
        val map = useOfflineMap(mapId, refreshKey)

        useEffect(mapView) {
            mapView.backgroundColorOverride = Color.rgb(127, 127, 127)
        }

        useEffect(zoomInButton, zoomOutButton, mapView) {
            CustomUiUtils.setButtonState(zoomInButton, false)
            CustomUiUtils.setButtonState(zoomOutButton, false)

            zoomInButton.setOnClickListener { mapView.zoom(2f) }
            zoomOutButton.setOnClickListener { mapView.zoom(0.5f) }
        }

        useEffect(title, map) {
            map?.let { title.title.text = it.name }
        }

        useEffect(title, map) {
            title.rightButton.setOnClickListener {
                val currentMap = map ?: return@setOnClickListener
                Pickers.menu(
                    it,
                    listOf(
                        getString(R.string.rename),
                        getString(R.string.attribution),
                        getString(R.string.delete)
                    )
                ) { index ->
                    when (index) {
                        0 -> rename(currentMap, refresh)
                        1 -> editAttribution(currentMap, refresh)
                        2 -> delete(currentMap)
                    }
                    true
                }
            }
        }

        useEffectWithCleanup(context, mapView, map) {
            if (map == null) {
                mapView.setLayers(emptyList())
                return@useEffectWithCleanup {}
            }
            val factory = LayerFactory()
            val layer = Tools.getMapLayerDefinition(context, MapsforgeTileSource.SOURCE_ID)!!
            val tileLayer = factory.createLayer(layer)
            tileLayer.setFeatureFilter(map.id.toString())
            mapView.setLayers(listOf(tileLayer))
            mapView.start()
            map.bounds?.let { bounds ->
                mapView.doOnLayout {
                    mapView.fitIntoView(bounds)
                }
            }
            return@useEffectWithCleanup {
                mapView.stop()
            }
        }
    }

    private fun useOfflineMap(mapId: Long, refreshKey: String): TrailMap? {
        val service = useService<OfflineMapService>()
        val (map, setMap) = useState<TrailMap?>(null)

        useBackgroundEffect(mapId, refreshKey, lifecycleHookTrigger.onResume()) {
            setMap(service.getTrailMap(mapId))
        }

        return map
    }

    private fun rename(map: TrailMap, onRenamed: () -> Unit) {
        inBackground {
            RenameMapCommand(requireContext(), getAppService()).execute(map)
            onMain {
                onRenamed()
            }
        }
    }

    private fun editAttribution(map: TrailMap, onUpdated: () -> Unit) {
        inBackground {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            onMain {
                onUpdated()
            }
        }
    }

    private fun delete(map: TrailMap) {
        inBackground {
            val service = getAppService<OfflineMapService>()
            DeleteMapCommand(requireContext(), getAppService()).execute(map)
            if (service.getTrailMap(map.id) == null) {
                findNavController().popBackStack()
            }
        }
    }
}
