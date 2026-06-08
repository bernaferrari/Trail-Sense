package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import kotlin.math.absoluteValue

class PhotoMapsFragment : TrailSenseAndromedaComposeFragment() {

    private val service = getAppService<MapService>()
    private val formatter = getAppService<FormatService>()

    private var mapId = 0L
    private var autoLockLocation = false
    private var map: PhotoMap? = null
    private var currentFragment: Fragment? = null
    private var title by mutableStateOf("")
    private var subtitle by mutableStateOf<String?>(null)
    private var showRecenter by mutableStateOf(false)
    private var childSpec by mutableStateOf<PhotoMapChildSpec?>(null)

    private val exportService by lazy { FragmentMapExportService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
        autoLockLocation = requireArguments().getBoolean("autoLockLocation", false)
    }

    @Composable
    override fun FragmentContent() {
        PhotoMapsScreen(
            title = title,
            subtitle = subtitle,
            showRecenter = showRecenter,
            onRecenter = ::recenter,
            onMenuClick = ::showMenu,
            childSpec = childSpec,
            onWarpFragment = ::onWarpFragment,
            onCalibrationFragment = ::onCalibrationFragment,
            onViewFragment = ::onViewFragment,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inBackground {
            loadMap()
        }
    }

    override fun onDestroyView() {
        currentFragment = null
        super.onDestroyView()
    }

    private fun showMenu(anchor: View) {
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
            anchor,
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

    private fun openGuide() {
        UserGuideUtils.openGuide(this, R.raw.guide_tool_offline_maps)
    }

    private fun calibrate() {
        showRecenter = true
        val fragment = PhotoMapCalibrationFragment.create(mapId, this::showRotation) {
            inBackground {
                autoRotate()
                loadMap()
            }
        }
        setFragment(fragment)
    }

    private fun showRotation(rotation: Float) {
        subtitle = getString(R.string.rotation_amount, formatter.formatDegrees(rotation))
    }

    private fun hideRotation() {
        subtitle = null
    }

    private fun delete() {
        inBackground {
            map?.let {
                DeleteMapCommand(requireContext(), service).execute(it)
                findNavController().popBackStack()
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
                    val renamed = service.getPhotoMap(updated.id)
                    onMain {
                        map = renamed
                        title = renamed?.name.orEmpty()
                    }
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
                        val changed = service.setProjection(updated, newProjection)
                        onMain {
                            map = changed
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
        val loaded = service.getPhotoMap(mapId)
        onMain {
            loaded?.let(::onMapLoad)
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        title = map.name
        when {
            !map.georeference.isWarpingCompleted -> warp()
            !map.isCalibrated -> calibrate()
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
            showRecenter = false
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
        if (!updatedMap.isCalibrated) return
        val newRotation = MapRotationCalculator().calculate(updatedMap)

        val delta = Trigonometry.deltaAngle(newRotation, updatedMap.georeference.rotation).absoluteValue
        if (delta > 1f) {
            toast(getString(R.string.map_auto_rotated))
        }

        val rotated = updatedMap.copy(
            georeference = updatedMap.georeference.copy(
                rotation = newRotation
            )
        )

        service.add(rotated)
        onMain {
            map = rotated
        }
    }


    private fun view() {
        hideRotation()
        showRecenter = true
        val fragment = ViewPhotoMapFragment.create(mapId, autoLockLocation)
        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment) {
        childSpec = when (fragment) {
            is WarpMapFragment -> PhotoMapChildSpec.Warp(mapId)
            is PhotoMapCalibrationFragment -> PhotoMapChildSpec.Calibration(mapId)
            is ViewPhotoMapFragment -> PhotoMapChildSpec.View(mapId, autoLockLocation)
            else -> null
        }
    }

    private fun onWarpFragment(fragment: WarpMapFragment) {
        currentFragment = fragment
        fragment.setOnCompleteListener {
            inBackground {
                loadMap()
            }
        }
    }

    private fun onCalibrationFragment(fragment: PhotoMapCalibrationFragment) {
        currentFragment = fragment
        fragment.setOnRotationListener(::showRotation)
        fragment.setOnCompleteListener {
            inBackground {
                autoRotate()
                loadMap()
            }
        }
    }

    private fun onViewFragment(fragment: ViewPhotoMapFragment) {
        currentFragment = fragment
    }
}

@Composable
private fun PhotoMapsScreen(
    title: String,
    subtitle: String?,
    showRecenter: Boolean,
    onRecenter: () -> Unit,
    onMenuClick: (View) -> Unit,
    childSpec: PhotoMapChildSpec?,
    onWarpFragment: (WarpMapFragment) -> Unit,
    onCalibrationFragment: (PhotoMapCalibrationFragment) -> Unit,
    onViewFragment: (ViewPhotoMapFragment) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhotoMapHeader(
                title = title,
                subtitle = subtitle,
                showRecenter = showRecenter,
                onRecenter = onRecenter,
                onMenuClick = onMenuClick
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_fragment")
            ) {
                childSpec?.let { spec ->
                    key(spec.key) {
                        when (spec) {
                            is PhotoMapChildSpec.Warp -> AndroidFragment<WarpMapFragment>(
                                modifier = Modifier.fillMaxSize(),
                                arguments = Bundle().apply {
                                    putLong("mapId", spec.mapId)
                                },
                                onUpdate = onWarpFragment
                            )

                            is PhotoMapChildSpec.Calibration -> AndroidFragment<PhotoMapCalibrationFragment>(
                                modifier = Modifier.fillMaxSize(),
                                arguments = Bundle().apply {
                                    putLong("mapId", spec.mapId)
                                },
                                onUpdate = onCalibrationFragment
                            )

                            is PhotoMapChildSpec.View -> AndroidFragment<ViewPhotoMapFragment>(
                                modifier = Modifier.fillMaxSize(),
                                arguments = Bundle().apply {
                                    putLong("mapId", spec.mapId)
                                    putBoolean("autoLockLocation", spec.autoLockLocation)
                                },
                                onUpdate = onViewFragment
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoMapHeader(
    title: String,
    subtitle: String?,
    showRecenter: Boolean,
    onRecenter: () -> Unit,
    onMenuClick: (View) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRecenter) {
                HeaderImageButton(
                    icon = R.drawable.ic_recenter,
                    tag = "map_recenter_btn",
                    onClick = { _ -> onRecenter() }
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("map_title")
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HeaderImageButton(
                icon = R.drawable.ic_menu_dots,
                tag = "map_menu_btn",
                onClick = onMenuClick
            )
        }
    }
}

@Composable
private fun HeaderImageButton(
    icon: Int,
    tag: String,
    onClick: (View) -> Unit
) {
    val anchor = LocalView.current
    IconButton(
        onClick = { onClick(anchor) },
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private sealed class PhotoMapChildSpec(val key: String) {
    data class Warp(val mapId: Long) : PhotoMapChildSpec("warp-$mapId")
    data class Calibration(val mapId: Long) : PhotoMapChildSpec("calibration-$mapId")
    data class View(val mapId: Long, val autoLockLocation: Boolean) :
        PhotoMapChildSpec("view-$mapId-$autoLockLocation")
}

@Preview(showBackground = true)
@Composable
private fun PhotoMapsScreenPreview() {
    TrailSenseTheme {
        PhotoMapsScreen(
            title = "Topo Survey",
            subtitle = "Rotation: 2°",
            showRecenter = true,
            onRecenter = {},
            onMenuClick = {},
            childSpec = null,
            onWarpFragment = {},
            onCalibrationFragment = {},
            onViewFragment = {}
        )
    }
}
