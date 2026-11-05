package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import kotlin.math.absoluteValue

@Composable
fun PhotoMapsToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { PhotoMapsController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier)
}

private class PhotoMapsController(
    private val host: TrailSenseDestinationHost
) {

    private val service = getAppService<MapService>()
    private val formatter = getAppService<FormatService>()

    private var mapId = host.destinationArguments?.getLong("mapId") ?: error("Missing map id")
    private var autoLockLocation =
        host.destinationArguments?.getBoolean("autoLockLocation", false) == true
    private var map: PhotoMap? = null
    private var currentViewController: ViewPhotoMapController? = null
    private var currentCalibrationController: PhotoMapCalibrationController? = null
    private var title by mutableStateOf("")
    private var subtitle by mutableStateOf<String?>(null)
    private var showRecenter by mutableStateOf(false)
    private var childSpec by mutableStateOf<PhotoMapChildSpec?>(null)

    private val exportService by lazy { PhotoMapExportService(host) }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        PhotoMapsScreen(
            title = title,
            subtitle = subtitle,
            showRecenter = showRecenter,
            onRecenter = ::recenter,
            onMenuClick = ::showMenu,
            childSpec = childSpec,
            host = host,
            onWarpComplete = {
                host.inBackground {
                    loadMap()
                }
            },
            onCalibrationRotation = ::showRotation,
            onCalibrationController = ::onCalibrationController,
            onCalibrationComplete = {
                host.inBackground {
                    autoRotate()
                    loadMap()
                }
            },
            onViewController = ::onViewController,
            modifier = modifier
        )
    }

    fun onCreate() {
        host.inBackground {
            loadMap()
        }
    }

    fun onDestroy() {
        currentViewController = null
        currentCalibrationController = null
    }

    private fun showMenu(anchor: View) {
        val isMapView = currentViewController != null
        val isCalibrationFragment = currentCalibrationController != null

        val actions = listOf(
            MapContextualAction.Calibrate to if (isMapView) host.getString(R.string.calibrate) else null,
            MapContextualAction.Guide to host.getString(R.string.tool_user_guide_title),
            MapContextualAction.Rename to host.getString(R.string.rename),
            MapContextualAction.ChangeProjection to if (isMapView || isCalibrationFragment) host.getString(
                R.string.change_map_projection
            ) else null,
            MapContextualAction.Measure to if (isMapView) host.getString(R.string.measure) else null,
            MapContextualAction.CreatePath to if (isMapView) host.getString(R.string.create_path) else null,
            MapContextualAction.AdjustLayers to if (isMapView) host.getString(R.string.layers) else null,
            MapContextualAction.Export to if (isMapView) host.getString(R.string.export) else null,
            MapContextualAction.Print to if (isMapView && Printer.canPrint()) host.getString(R.string.print) else null,
            MapContextualAction.Delete to host.getString(R.string.delete),
            MapContextualAction.Trace to if (isMapView) host.getString(R.string.trace) else null
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
        UserGuideUtils.openGuide(
            host.requireContext(),
            host.findNavigator(),
            R.raw.guide_tool_offline_maps
        )
    }

    private fun calibrate() {
        showRecenter = true
        currentViewController = null
        childSpec = PhotoMapChildSpec.Calibration(mapId)
    }

    private fun showRotation(rotation: Float) {
        subtitle = host.getString(R.string.rotation_amount, formatter.formatDegrees(rotation))
    }

    private fun hideRotation() {
        subtitle = null
    }

    private fun delete() {
        host.inBackground {
            map?.let {
                DeleteMapCommand(host.requireContext(), service).execute(it)
                host.findNavigator().popBackStack()
            }
        }
    }

    private fun print() {
        val command = PrintMapCommand(host.requireContext())
        host.inBackground(BackgroundMinimumState.Created) {
            map?.let {
                command.execute(it)
            }
        }
    }

    private fun export() {
        host.inBackground {
            map?.let {
                service.getPhotoMap(it.id)?.let { updated ->
                    exportService.export(updated)
                }
            }
        }
    }

    private fun measure() {
        currentViewController?.startDistanceMeasurement(emptyArray())
    }

    private fun trace() {
        currentViewController?.trace()
    }

    private fun adjustLayers() {
        currentViewController?.adjustLayers()
    }

    private fun rename() {
        host.inBackground {
            map?.let {
                service.getPhotoMap(it.id)?.let { updated ->
                    RenameMapCommand(host.requireContext(), service).execute(updated)
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
            host.requireContext(),
            host.getString(R.string.change_map_projection),
            projectionNames,
            projections.indexOf(map?.georeference?.projectionType)
        ) {
            it ?: return@item
            val newProjection = projections[it]
            val applyProjectionChange = {
                map?.let { m ->
                    host.inBackground {
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
                    host.requireContext(),
                    host.getString(R.string.change_map_projection),
                    host.getString(R.string.map_calibration_clear_message)
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
        currentViewController?.recenter()

        currentCalibrationController?.recenter()
    }

    private fun reload() {
        currentViewController?.reloadMap()

        currentCalibrationController?.reloadMap()
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
        showRecenter = false
        currentViewController = null
        currentCalibrationController = null
        childSpec = PhotoMapChildSpec.Warp(mapId)
    }

    private suspend fun autoRotate() {
        val updatedMap = service.getPhotoMap(mapId) ?: return
        if (!updatedMap.isCalibrated) return
        val newRotation = MapRotationCalculator().calculate(updatedMap)

        val delta = Trigonometry.deltaAngle(newRotation, updatedMap.georeference.rotation).absoluteValue
        if (delta > 1f) {
            host.toast(host.getString(R.string.map_auto_rotated))
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
        currentCalibrationController = null
        childSpec = PhotoMapChildSpec.View(mapId, autoLockLocation)
    }

    private fun onCalibrationController(controller: PhotoMapCalibrationController) {
        currentCalibrationController = controller
    }

    private fun onViewController(controller: ViewPhotoMapController) {
        currentViewController = controller
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
    host: TrailSenseDestinationHost?,
    onWarpComplete: () -> Unit,
    onCalibrationRotation: (Float) -> Unit,
    onCalibrationController: (PhotoMapCalibrationController) -> Unit,
    onCalibrationComplete: () -> Unit,
    onViewController: (ViewPhotoMapController) -> Unit,
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
                if (host != null) {
                    childSpec?.let { spec ->
                        key(spec.key) {
                            when (spec) {
                                is PhotoMapChildSpec.Warp -> WarpMapToolScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    host = host,
                                    mapId = spec.mapId,
                                    onComplete = onWarpComplete
                                )

                                is PhotoMapChildSpec.Calibration -> PhotoMapCalibrationToolScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    host = host,
                                    mapId = spec.mapId,
                                    onRotation = onCalibrationRotation,
                                    onComplete = onCalibrationComplete,
                                    onController = onCalibrationController
                                )

                                is PhotoMapChildSpec.View -> ViewPhotoMapToolScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    host = host,
                                    mapId = spec.mapId,
                                    autoLockLocation = spec.autoLockLocation,
                                    onController = onViewController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The map's title in the standard instrument bar: back, the map's name and rotation, recenter and the menu. */
@Composable
private fun PhotoMapHeader(
    title: String,
    subtitle: String?,
    showRecenter: Boolean,
    onRecenter: () -> Unit,
    onMenuClick: (View) -> Unit
) {
    val anchor = LocalView.current
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentTopBar(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.testTag("map_title"),
        actions = {
            if (showRecenter) {
                IconButton(onClick = onRecenter, modifier = Modifier.testTag("map_recenter_btn")) {
                    Icon(rememberVectorPainter(TrailIcons.Recenter), contentDescription = stringResource(R.string.center))
                }
            }
            IconButton(onClick = { onMenuClick(anchor) }, modifier = Modifier.testTag("map_menu_btn")) {
                Icon(rememberVectorPainter(TrailIcons.MenuDots), contentDescription = stringResource(R.string.map_options))
            }
        }
    )
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
            host = null,
            onWarpComplete = {},
            onCalibrationRotation = {},
            onCalibrationController = {},
            onCalibrationComplete = {},
            onViewController = {}
        )
    }
}
