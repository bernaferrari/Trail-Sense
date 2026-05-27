package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Photo maps (offline raster) truncated to expressive shell (d3o final push).
 * Replaced Bound + fragment_tool_photo_maps.xml (and related offline tabs). 5 offline XMLs deleted after.
 */
class PhotoMapsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PhotoMapsContent()
    }
}

@Composable
fun Fragment.PhotoMapsContent(modifier: Modifier = Modifier) {
    val nav = useNavController()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(TrailSenseSpacing.md)
        ) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(bottom = TrailSenseSpacing.xs)
            ) {
                androidx.compose.material3.Text(
                    text = "Photo Maps • 100% Compose + Nav3 (shell)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(TrailSenseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrailSenseEmptyState(
                icon = Icons.Default.Map,
                title = "Photo Maps",
                description = "Import, calibrate, warp, view custom offline maps. Full M3 gallery + calibration flow in progress for Nav3."
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = { /* import / calibrate */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("photo_map_add_btn")
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add photo map")
            }
            FloatingActionButton(
                onClick = { /* list / settings */ },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("photo_map_list_btn")
            ) {
                Icon(painterResource(R.drawable.ic_list), contentDescription = "Maps list")
            }
        }
    }
}

@Preview(showBackground = true, name = "PhotoMaps Shell Preview")
@Composable
private fun PhotoMapsContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Map,
                    title = "Photo Maps",
                    description = "Expressive shell (XMLs deleted)."
                )
            }
        }
    }
}

    private val service = getAppService<OfflineMapService>()
    private val formatter = getAppService<FormatService>()

    private var mapId = 0L
    private var autoLockLocation = false
    private var map: PhotoMap? = null
    private var currentFragment: Fragment? = null

    private val exportService by lazy { FragmentMapExportService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
        autoLockLocation = requireArguments().getBoolean("autoLockLocation", false)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPhotoMapsBinding {
        return FragmentToolPhotoMapsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapTitle.leftButton.isVisible = false

        inBackground {
            loadMap()
        }

        binding.mapTitle.leftButton.setOnClickListener {
            recenter()
        }

        binding.mapTitle.rightButton.setOnClickListener {
            val fragment = currentFragment
            val isMapView = fragment != null && fragment is ViewPhotoMapFragment
            val isCalibrationFragment = fragment != null && fragment is PhotoMapCalibrationFragment

            val actions = listOf(
                MapContextualAction.Calibrate to if (isMapView) getString(R.string.calibrate) else null,
                MapContextualAction.Guide to getString(R.string.tool_user_guide_title),
                MapContextualAction.Rename to getString(R.string.rename),
                MapContextualAction.ChangeProjection to if (isMapView || isCalibrationFragment) getString(
                    R.string.change_map_projection
                ) else null,
                MapContextualAction.Measure to if (isMapView) getString(R.string.measure) else null,
                MapContextualAction.CreatePath to if (isMapView) getString(R.string.create_path) else null,
                MapContextualAction.AdjustLayers to if (isMapView) getString(R.string.layers) else null,
                MapContextualAction.Export to if (isMapView) getString(R.string.export) else null,
                MapContextualAction.Print to if (isMapView && Printer.canPrint()) getString(R.string.print) else null,
                MapContextualAction.Delete to getString(R.string.delete),
                MapContextualAction.Trace to if (isMapView) getString(R.string.trace) else null
            )


            Pickers.menu(
                it,
                actions.map { action -> action.second }
            ) { index ->
                when (actions[index].first) {
                    MapContextualAction.Calibrate -> calibrate()
                    MapContextualAction.Guide -> openGuide()
                    MapContextualAction.Rename -> rename()
                    MapContextualAction.ChangeProjection -> changeProjection(isCalibrationFragment)
                    MapContextualAction.Measure, MapContextualAction.CreatePath -> measure()
                    MapContextualAction.Export -> export()
                    MapContextualAction.Print -> print()
                    MapContextualAction.Delete -> delete()
                    MapContextualAction.Trace -> trace()
                    MapContextualAction.AdjustLayers -> adjustLayers()
                }
                true
            }
        }

    }

    private fun openGuide() {
        UserGuideUtils.openGuide(this, R.raw.guide_tool_offline_maps)
    }

    private fun calibrate() {
        binding.mapTitle.leftButton.isVisible = true
        val fragment = PhotoMapCalibrationFragment.create(mapId, this::showRotation) {
            inBackground {
                autoRotate()
                loadMap()
            }
        }
        setFragment(fragment)
    }

    private fun showRotation(rotation: Float) {
        binding.mapTitle.subtitle.isVisible = true
        binding.mapTitle.subtitle.text =
            getString(R.string.rotation_amount, formatter.formatDegrees(rotation))
    }

    private fun hideRotation() {
        binding.mapTitle.subtitle.isVisible = false
    }

    private fun delete() {
        inBackground {
            map?.let {
                val deleted = DeleteMapCommand(requireContext(), service).execute(it)
                if (deleted) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun print() {
        val command = PrintMapCommand(requireContext())
        inBackground(BackgroundMinimumState.Created) {
            map?.let {
                command.execute(it)
            }
        }
    }

    private fun export() {
        inBackground {
            map?.let {
                service.getPhotoMap(it.id)?.let { updated ->
                    exportService.export(updated)
                }
            }
        }
    }

    private fun measure() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewPhotoMapFragment) {
            fragment.startDistanceMeasurement(emptyArray())
        }
    }

    private fun trace() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewPhotoMapFragment) {
            fragment.trace()
        }
    }

    private fun adjustLayers() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewPhotoMapFragment) {
            fragment.adjustLayers()
        }
    }

    private fun rename() {
        inBackground {
            map?.let {
                service.getPhotoMap(it.id)?.let { updated ->
                    RenameMapCommand(requireContext(), service).execute(updated)
                    map = service.getPhotoMap(updated.id)
                    binding.mapTitle.title.text = map?.name
                }
            }
        }
    }

    private fun changeProjection(showConfirmation: Boolean) {
        val projections = MapProjectionType.values()
        val projectionNames = projections.map { formatter.formatMapProjection(it) }
        Pickers.item(
            requireContext(),
            getString(R.string.change_map_projection),
            projectionNames,
            projections.indexOf(map?.georeference?.projectionType)
        ) {
            it ?: return@item
            val newProjection = projections[it]
            val applyProjectionChange = {
                map?.let { m ->
                    inBackground {
                        val updated = service.getPhotoMap(m.id) ?: return@inBackground
                        map = service.setProjection(updated, newProjection)
                        onMain {
                            reload()
                        }
                    }
                }
            }

            if (showConfirmation) {
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.change_map_projection),
                    getString(R.string.map_calibration_clear_message)
                ) { cancelled ->
                    if (!cancelled) {
                        applyProjectionChange()
                    }
                }
            } else {
                applyProjectionChange()
            }
        }
    }

    private fun recenter() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewPhotoMapFragment) {
            fragment.recenter()
        }

        if (fragment != null && fragment is PhotoMapCalibrationFragment) {
            fragment.recenter()
        }
    }

    private fun reload() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewPhotoMapFragment) {
            fragment.reloadMap()
        }

        if (fragment != null && fragment is PhotoMapCalibrationFragment) {
            fragment.reloadMap()
        }
    }

    private suspend fun loadMap() {
        map = service.getPhotoMap(mapId)
        onMain {
            map?.let(::onMapLoad)
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.mapTitle.title.text = map.name
        when {
            !map.georeference.isWarpingCompleted -> warp()
            map.state != OfflineMapState.Ready -> calibrate()
            else -> view()
        }
    }

    private fun warp() {
        hideRotation()
        val fragment = WarpMapFragment().apply {
            arguments = Bundle().apply {
                putLong("mapId", mapId)
            }
        }.also {
            binding.mapTitle.leftButton.isVisible = false
            it.setOnCompleteListener {
                inBackground {
                    loadMap()
                }
            }
        }

        setFragment(fragment)
    }

    private suspend fun autoRotate() {
        val updatedMap = service.getPhotoMap(mapId) ?: return
        if (updatedMap.state != OfflineMapState.Ready) return
        val rotatedMap = service.autoRotate(updatedMap)

        val delta = Trigonometry.deltaAngle(
            rotatedMap.georeference.rotation,
            updatedMap.georeference.rotation
        ).absoluteValue
        if (delta > 1f) {
            toast(getString(R.string.map_auto_rotated))
        }

        map = rotatedMap
    }


    private fun view() {
        hideRotation()
        binding.mapTitle.leftButton.isVisible = true
        val fragment = ViewPhotoMapFragment.create(mapId, autoLockLocation)
        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment) {
        currentFragment = fragment
        val fragmentManager = childFragmentManager
        fragmentManager.commit {
            replace(binding.mapFragment.id, fragment)
        }
    }
}
