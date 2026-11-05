package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun PhotoMapsToolScreen(
    route: Route.PhotoMaps,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val formatter = remember { getAppService<FormatService>() }
    val viewModel = viewModel {
        PhotoMapsViewModel(getAppService(), route.mapId, route.autoLockLocation)
    }
    val exportService = remember(context, effects, lifecycleOwner) {
        // Exporting picks a file through the activity's result launcher (an Activity-only API).
        PhotoMapExportService(context, requireNotNull(context.findMainActivity()), effects, lifecycleOwner)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PhotoMapsEvent.MapAutoRotated -> effects.showMessage(context.getString(R.string.map_auto_rotated))
            }
        }
    }

    fun onMenuAction(action: MapContextualAction) {
        val map = viewModel.map
        when (action) {
            MapContextualAction.Calibrate -> viewModel.calibrate()
            MapContextualAction.Guide -> UserGuideUtils.openGuide(context, navigator, R.raw.guide_tool_offline_maps)
            MapContextualAction.Rename -> map?.let {
                scope.launch {
                    viewModel.getLatest(it)?.let { latest ->
                        RenameMapCommand(context, getAppService()).execute(latest)
                        viewModel.reloadName(latest.id)
                    }
                }
            }

            MapContextualAction.ChangeProjection -> changeProjection(
                context,
                formatter,
                current = map?.georeference?.projectionType,
                showConfirmation = viewModel.childSpec is PhotoMapChildSpec.Calibration,
                onChange = viewModel::setProjection
            )

            MapContextualAction.Measure, MapContextualAction.CreatePath ->
                viewModel.sendToChild(PhotoMapChildCommand.Measure)

            MapContextualAction.Export -> map?.let {
                scope.launch { viewModel.getLatest(it)?.let(exportService::export) }
            }

            MapContextualAction.Print -> map?.let {
                scope.launch { PrintMapCommand(context).execute(it) }
            }

            MapContextualAction.Delete -> map?.let {
                scope.launch {
                    DeleteMapCommand(context, getAppService()).execute(it)
                    navigator.popBackStack()
                }
            }

            MapContextualAction.Trace -> viewModel.sendToChild(PhotoMapChildCommand.Trace)
            MapContextualAction.AdjustLayers -> viewModel.sendToChild(PhotoMapChildCommand.AdjustLayers)
        }
    }

    PhotoMapsScreen(
        title = viewModel.title,
        subtitle = viewModel.rotation?.let {
            stringResource(R.string.rotation_amount, formatter.formatDegrees(it))
        },
        showRecenter = viewModel.showRecenter,
        onRecenter = { viewModel.sendToChild(PhotoMapChildCommand.Recenter) },
        menuActions = getMenuActions(viewModel.childSpec),
        onMenuAction = ::onMenuAction,
        childSpec = viewModel.childSpec,
        childKey = viewModel.childGeneration,
        childCommands = viewModel.childCommands,
        onWarpComplete = viewModel::onWarpComplete,
        onCalibrationRotation = viewModel::showRotation,
        onCalibrationComplete = viewModel::onCalibrationComplete,
        modifier = modifier
    )
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}

private fun changeProjection(
    context: Context,
    formatter: FormatService,
    current: MapProjectionType?,
    showConfirmation: Boolean,
    onChange: (MapProjectionType) -> Unit
) {
    val projections = MapProjectionType.entries
    val projectionNames = projections.map { formatter.formatMapProjection(it) }
    Pickers.item(
        context,
        context.getString(R.string.change_map_projection),
        projectionNames,
        projections.indexOf(current)
    ) {
        it ?: return@item
        val newProjection = projections[it]
        if (showConfirmation) {
            Alerts.dialog(
                context,
                context.getString(R.string.change_map_projection),
                context.getString(R.string.map_calibration_clear_message)
            ) { cancelled ->
                if (!cancelled) {
                    onChange(newProjection)
                }
            }
        } else {
            onChange(newProjection)
        }
    }
}

/** The menu's actions for the open child screen, in order. */
private fun getMenuActions(childSpec: PhotoMapChildSpec?): List<MapContextualAction> {
    val isMapView = childSpec is PhotoMapChildSpec.View
    val isCalibration = childSpec is PhotoMapChildSpec.Calibration
    return listOfNotNull(
        MapContextualAction.Calibrate.takeIf { isMapView },
        MapContextualAction.Guide,
        MapContextualAction.Rename,
        MapContextualAction.ChangeProjection.takeIf { isMapView || isCalibration },
        MapContextualAction.Measure.takeIf { isMapView },
        MapContextualAction.CreatePath.takeIf { isMapView },
        MapContextualAction.AdjustLayers.takeIf { isMapView },
        MapContextualAction.Export.takeIf { isMapView },
        MapContextualAction.Print.takeIf { isMapView && Printer.canPrint() },
        MapContextualAction.Delete,
        MapContextualAction.Trace.takeIf { isMapView }
    )
}

private fun MapContextualAction.label(): Int {
    return when (this) {
        MapContextualAction.Calibrate -> R.string.calibrate
        MapContextualAction.Guide -> R.string.tool_user_guide_title
        MapContextualAction.Rename -> R.string.rename
        MapContextualAction.ChangeProjection -> R.string.change_map_projection
        MapContextualAction.Measure -> R.string.measure
        MapContextualAction.CreatePath -> R.string.create_path
        MapContextualAction.AdjustLayers -> R.string.layers
        MapContextualAction.Export -> R.string.export
        MapContextualAction.Print -> R.string.print
        MapContextualAction.Delete -> R.string.delete
        MapContextualAction.Trace -> R.string.trace
    }
}

/** What the open child screen (view or calibration) is asked to do from the title bar. */
internal enum class PhotoMapChildCommand {
    Recenter,
    Reload,
    Measure,
    Trace,
    AdjustLayers
}

internal enum class PhotoMapsEvent {
    MapAutoRotated
}

internal class PhotoMapsViewModel(
    private val service: MapService,
    private val mapId: Long,
    private val autoLockLocation: Boolean
) : ViewModel() {

    var map by mutableStateOf<PhotoMap?>(null)
        private set
    var title by mutableStateOf("")
        private set

    /** The calibration's rotation from the nearest quarter turn, shown under the title while calibrating. */
    var rotation by mutableStateOf<Float?>(null)
        private set
    var showRecenter by mutableStateOf(false)
        private set
    var childSpec by mutableStateOf<PhotoMapChildSpec?>(null)
        private set

    /** Changes each time a child screen opens, so it starts with fresh state. */
    var childGeneration by mutableIntStateOf(0)
        private set

    private val _childCommands = MutableSharedFlow<PhotoMapChildCommand>(extraBufferCapacity = 8)
    val childCommands: Flow<PhotoMapChildCommand> = _childCommands.asSharedFlow()

    private val _events = Channel<PhotoMapsEvent>(Channel.BUFFERED)
    val events: Flow<PhotoMapsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadMap() }
    }

    fun sendToChild(command: PhotoMapChildCommand) {
        _childCommands.tryEmit(command)
    }

    fun onWarpComplete() {
        viewModelScope.launch { loadMap() }
    }

    fun onCalibrationComplete() {
        viewModelScope.launch {
            autoRotate()
            loadMap()
        }
    }

    fun showRotation(rotation: Float) {
        this.rotation = rotation
    }

    fun calibrate() {
        showRecenter = true
        openChild(PhotoMapChildSpec.Calibration(mapId))
    }

    suspend fun getLatest(map: PhotoMap): PhotoMap? {
        return service.getPhotoMap(map.id)
    }

    fun reloadName(id: Long) {
        viewModelScope.launch {
            val renamed = service.getPhotoMap(id)
            map = renamed
            title = renamed?.name.orEmpty()
        }
    }

    fun setProjection(projection: MapProjectionType) {
        val current = map ?: return
        viewModelScope.launch {
            val updated = service.getPhotoMap(current.id) ?: return@launch
            map = service.setProjection(updated, projection)
            sendToChild(PhotoMapChildCommand.Reload)
        }
    }

    private suspend fun loadMap() {
        val loaded = service.getPhotoMap(mapId) ?: return
        map = loaded
        title = loaded.name
        when {
            !loaded.georeference.isWarpingCompleted -> {
                rotation = null
                showRecenter = false
                openChild(PhotoMapChildSpec.Warp(mapId))
            }

            !loaded.isCalibrated -> calibrate()
            else -> {
                rotation = null
                showRecenter = true
                openChild(PhotoMapChildSpec.View(mapId, autoLockLocation))
            }
        }
    }

    private fun openChild(spec: PhotoMapChildSpec) {
        childSpec = spec
        childGeneration++
    }

    private suspend fun autoRotate() {
        val updatedMap = service.getPhotoMap(mapId) ?: return
        if (!updatedMap.isCalibrated) return
        val newRotation = MapRotationCalculator().calculate(updatedMap)

        val delta = Trigonometry.deltaAngle(newRotation, updatedMap.georeference.rotation).absoluteValue
        if (delta > 1f) {
            _events.send(PhotoMapsEvent.MapAutoRotated)
        }

        val rotated = updatedMap.copy(
            georeference = updatedMap.georeference.copy(
                rotation = newRotation
            )
        )

        service.add(rotated)
        map = rotated
    }
}

@Composable
private fun PhotoMapsScreen(
    title: String,
    subtitle: String?,
    showRecenter: Boolean,
    onRecenter: () -> Unit,
    menuActions: List<MapContextualAction>,
    onMenuAction: (MapContextualAction) -> Unit,
    childSpec: PhotoMapChildSpec?,
    childKey: Int,
    childCommands: Flow<PhotoMapChildCommand>,
    onWarpComplete: () -> Unit,
    onCalibrationRotation: (Float) -> Unit,
    onCalibrationComplete: () -> Unit,
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
                menuActions = menuActions,
                onMenuAction = onMenuAction
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_fragment")
            ) {
                childSpec?.let { spec ->
                    key(spec.key, childKey) {
                        val viewModelKey = "${spec.key}-$childKey"
                        when (spec) {
                            is PhotoMapChildSpec.Warp -> WarpMapToolScreen(
                                modifier = Modifier.fillMaxSize(),
                                mapId = spec.mapId,
                                viewModelKey = viewModelKey,
                                onComplete = onWarpComplete
                            )

                            is PhotoMapChildSpec.Calibration -> PhotoMapCalibrationToolScreen(
                                modifier = Modifier.fillMaxSize(),
                                mapId = spec.mapId,
                                viewModelKey = viewModelKey,
                                commands = childCommands,
                                onRotation = onCalibrationRotation,
                                onComplete = onCalibrationComplete
                            )

                            is PhotoMapChildSpec.View -> ViewPhotoMapToolScreen(
                                modifier = Modifier.fillMaxSize(),
                                mapId = spec.mapId,
                                viewModelKey = viewModelKey,
                                autoLockLocation = spec.autoLockLocation,
                                commands = childCommands
                            )
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
    menuActions: List<MapContextualAction>,
    onMenuAction: (MapContextualAction) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
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
            Box {
                IconButton(onClick = { isMenuOpen = true }, modifier = Modifier.testTag("map_menu_btn")) {
                    Icon(
                        rememberVectorPainter(TrailIcons.MenuDots),
                        contentDescription = stringResource(R.string.map_options)
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    menuActions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(stringResource(action.label())) },
                            onClick = {
                                isMenuOpen = false
                                onMenuAction(action)
                            }
                        )
                    }
                }
            }
        }
    )
}

internal sealed class PhotoMapChildSpec(val key: String) {
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
            menuActions = getMenuActions(null),
            onMenuAction = {},
            childSpec = null,
            childKey = 0,
            childCommands = kotlinx.coroutines.flow.emptyFlow(),
            onWarpComplete = {},
            onCalibrationRotation = {},
            onCalibrationComplete = {}
        )
    }
}
