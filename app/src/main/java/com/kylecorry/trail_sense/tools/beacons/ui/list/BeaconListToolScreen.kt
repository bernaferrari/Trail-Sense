package com.kylecorry.trail_sense.tools.beacons.ui.list

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.extensions.filterIndices
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.CreateBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.RenameBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell.NavigateToNearestCellSignal
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.MostRecentBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import java.time.Instant

interface BeaconListControllerHost {
    fun refreshBeaconList()
}

@Composable
fun BeaconListToolScreen(
    fragment: Fragment,
    modifier: Modifier = Modifier,
    onController: (BeaconListControllerHost?) -> Unit = {}
) {
    val controller = remember(fragment) { BeaconListController(fragment) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        onController(controller)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onController(null)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class BeaconListController(private val fragment: Fragment) : BeaconListControllerHost {

    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(fragment.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val beaconService by lazy { BeaconService(fragment.requireContext()) }
    private val beaconLoader by lazy { BeaconLoader(beaconService, prefs.beacons) }
    private var sort = BeaconSortMethod.Closest

    private var initialLocation: GeoUri? = null
    private var beaconItems by mutableStateOf<List<IBeacon>>(emptyList())
    private var beaconListResetKey by mutableIntStateOf(0)
    private var title by mutableStateOf("")
    private var searchText by mutableStateOf("")
    private var sortMenuText by mutableStateOf("")
    private var isCreateMenuOpen by mutableStateOf(false)

    private val gpxService by lazy {
        IOFactory().createGpxService(fragment as AndromedaFragment)
    }

    private lateinit var manager: GroupListManager<IBeacon>
    private var lastRoot: IBeacon? = null
    private var isInitialized = false

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        BeaconListScreen(
            title = title.ifBlank { stringResource(R.string.beacons) },
            sortMenuText = sortMenuText.ifBlank {
                stringResource(R.string.sort_by, stringResource(R.string.closest))
            },
            searchText = searchText,
            items = beaconItems,
            isEmpty = beaconItems.isEmpty(),
            resetKey = beaconListResetKey,
            formatService = formatService,
            baseDistanceUnits = prefs.baseDistanceUnits,
            currentLocation = gps.location,
            isCreateMenuOpen = isCreateMenuOpen,
            onSearchChange = {
                searchText = it
                manager.search(it)
            },
            onToolbarAction = ::handleToolbarAction,
            onCreateMenuOpenChange = { isCreateMenuOpen = it },
            onCreateAction = ::handleCreateAction,
            onBeaconAction = ::handleBeaconAction,
            onGroupAction = ::handleBeaconGroupAction,
            modifier = modifier
        )
    }

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        val arguments = fragment.requireArguments()
        if (arguments.containsKey("initial_location")) {
            initialLocation = BundleCompat.getParcelable(
                arguments, "initial_location",
                GeoUri::class.java
            )
        }

        navController = fragment.findNavController()

        sort = prefs.beacons.beaconSort
        sortMenuText = fragment.getString(R.string.sort_by, getSortString(sort))

        manager = GroupListManager(
            fragment.viewLifecycleOwner.lifecycleScope,
            beaconLoader,
            lastRoot,
            this::sortBeacons
        )

        manager.onChange = { root, items, rootChanged ->
            beaconItems = items
            if (rootChanged) {
                beaconListResetKey++
            }
            title = root?.name ?: fragment.getString(R.string.beacons)
        }

        initialLocation?.let {
            initialLocation = null
            createBeacon(initialLocation = it)
        }

        fragment.onBackPressed {
            when {
                isCreateMenuOpen -> {
                    setCreateMenuVisibility(false)
                }

                else -> {
                    if (!manager.up()) {
                        remove()
                        navController.navigateUp()
                    }
                }
            }
        }
        title = fragment.getString(R.string.beacons)
    }

    private suspend fun sortBeacons(beacons: List<IBeacon>): List<IBeacon> {
        val method = when (sort) {
            BeaconSortMethod.MostRecent -> MostRecentBeaconSort(beaconService)
            BeaconSortMethod.Closest -> ClosestBeaconSort(beaconService, gps::location)
            BeaconSortMethod.Name -> NameBeaconSort()
        }
        return method.sort(beacons)
    }

    private fun setCreateMenuVisibility(isShowing: Boolean) {
        isCreateMenuOpen = isShowing
    }

    private fun handleToolbarAction(action: BeaconToolbarAction) {
        when (action) {
            BeaconToolbarAction.Sort -> changeSort()
            BeaconToolbarAction.Export -> onExportBeacons()
            BeaconToolbarAction.NavigateToNearestCellSignal -> navigateToNearestCellSignal()
            BeaconToolbarAction.Delete -> deleteCurrentGroup()
        }
    }

    private fun handleCreateAction(action: BeaconCreateAction) {
        when (action) {
            BeaconCreateAction.ImportQr -> {
                fragment.requestCamera { hasPermission ->
                    if (hasPermission) {
                        importBeaconFromQR()
                    } else {
                        fragment.alertNoCameraPermission()
                    }
                }
            }

            BeaconCreateAction.ImportGpx -> importBeacons()
            BeaconCreateAction.CreateGroup -> {
                val command =
                    CreateBeaconGroupCommand(fragment.requireContext(), fragment.viewLifecycleOwner.lifecycleScope, beaconService) {
                        refresh()
                    }
                command.execute(manager.root?.id)
            }

            BeaconCreateAction.CreateBeacon -> createBeacon(group = manager.root?.id)
        }
        setCreateMenuVisibility(false)
    }

    private fun onExportBeacons() {
        fragment.inBackground {
            val gpx = getExportGPX()
            onMain {
                Pickers.items(
                    fragment.requireContext(),
                    fragment.getString(R.string.export),
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
    }

    fun onResume() {
        if (!isInitialized) {
            return
        }
        manager.refresh()
        // Get a GPS reading
        gps.start(this::onLocationUpdate)
    }

    fun onPause() {
        if (!isInitialized) {
            return
        }
        gps.stop(this::onLocationUpdate)
        tryOrNothing {
            lastRoot = manager.root
        }
    }

    fun onDestroy() {
        onPause()
        isInitialized = false
    }

    private fun onLocationUpdate(): Boolean {
        return false
    }

    private fun importBeaconFromQR() {
        val encoder = BeaconQREncoder()
        CustomUiUtils.scanQR(fragment, fragment.getString(R.string.beacon_qr_import_instructions)) {
            if (it != null) {
                val beacon = encoder.decode(it) ?: return@scanQR true
                createBeacon(group = manager.root?.id, initialLocation = GeoUri.from(beacon))
            }
            false
        }
    }

    private fun changeSort() {
        val sortOptions = BeaconSortMethod.values()
        Pickers.item(
            fragment.requireContext(),
            fragment.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.beacons.beaconSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.beacons.beaconSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                sortMenuText = fragment.getString(R.string.sort_by, getSortString(sort))
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: BeaconSortMethod): String {
        return when (sortMethod) {
            BeaconSortMethod.MostRecent -> fragment.getString(R.string.most_recent)
            BeaconSortMethod.Closest -> fragment.getString(R.string.closest)
            BeaconSortMethod.Name -> fragment.getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private fun export(gpx: GPXData) {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            onMain {
                if (success) {
                    fragment.toast(
                        fragment.resources.getQuantityString(
                            R.plurals.beacons_exported,
                            gpx.waypoints.size,
                            gpx.waypoints.size
                        )
                    )
                } else {
                    fragment.toast(
                        fragment.getString(R.string.beacon_export_error)
                    )
                }
            }
        }
    }

    private fun importBeacons() {
        val importer = BeaconGpxImporter(fragment.requireContext())
        fragment.inBackground(BackgroundMinimumState.Created) {
            val gpx = gpxService.import()
            val waypoints = gpx?.waypoints ?: emptyList()
            onMain {
                Pickers.items(
                    fragment.requireContext(),
                    fragment.getString(R.string.import_btn),
                    waypoints.map {
                        it.name ?: formatService.formatLocation(it.coordinate)
                    },
                    List(waypoints.size) { it }
                ) {
                    if (it != null) {
                        fragment.inBackground {
                            val gpxToImport =
                                GPXData(waypoints.filterIndices(it), emptyList(), emptyList())
                            val count = onIO {
                                importer.import(gpxToImport, manager.root?.id)
                            }
                            onMain {
                                Alerts.toast(
                                    fragment.requireContext(),
                                    fragment.resources.getQuantityString(
                                        R.plurals.beacons_imported,
                                        count,
                                        count
                                    )
                                )
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getExportGPX(): GPXData = onIO {
        val all = beaconService.getBeacons(
            manager.root?.id,
            includeGroups = true,
            maxDepth = null,
            includeRoot = true
        )

        val groups = all.filterIsInstance<BeaconGroup>()
        val beacons = all.filterIsInstance<Beacon>()

        BeaconGpxConverter().toGPX(beacons, groups)
    }

    private fun createBeacon(
        group: Long? = null,
        initialLocation: GeoUri? = null,
        editingBeaconId: Long? = null
    ) {
        val bundle = Bundle()

        group?.let { bundle.putLong("initial_group", it) }
        initialLocation?.let { bundle.putParcelable("initial_location", it) }
        editingBeaconId?.let { bundle.putLong("edit_beacon", it) }

        navController.navigate(
            R.id.action_beaconListFragment_to_placeBeaconFragment,
            bundle
        )
    }

    override fun refreshBeaconList() {
        refresh()
    }

    private fun refresh() {
        manager.refresh()
    }

    private fun viewBeacon(id: Long) {
        val bundle = Bundle().apply {
            putLong("beacon_id", id)
        }
        navController.navigate(
            R.id.action_beacon_list_to_beaconDetailsFragment,
            bundle
        )
    }

    private fun navigate(beacon: Beacon) {
        val navigator = Navigator.getInstance(fragment.requireContext())
        navigator.navigateTo(beacon)
        navController.navigate(R.id.action_navigation)
    }

    private fun deleteCurrentGroup() {
        val group = manager.root
        delete(group as BeaconGroup?)
    }

    private fun delete(beacon: Beacon) {
        val command = DeleteBeaconCommand(
            fragment.requireContext(),
            fragment.viewLifecycleOwner.lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun delete(group: BeaconGroup?) {
        Alerts.dialog(
            fragment.requireContext(),
            fragment.getString(R.string.delete),
            group?.name?.let {
                fragment.getString(
                    R.string.delete_beacon_group_message,
                    it
                )
            } ?: fragment.getString(R.string.delete_all_beacons),
        ) { cancelled ->
            if (!cancelled) {
                fragment.inBackground {
                    onIO {
                        beaconService.delete(group)
                    }

                    onMain {
                        if (group == manager.root) {
                            manager.up()
                        }
                        refresh()
                    }
                }

            }
        }
    }

    private fun move(beacon: Beacon) {
        val command = MoveBeaconCommand(
            fragment.requireContext(),
            fragment.viewLifecycleOwner.lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun move(group: BeaconGroup) {
        val command =
            MoveBeaconGroupCommand(fragment.requireContext(), fragment.viewLifecycleOwner.lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun toggleVisibility(beacon: Beacon) {
        fragment.inBackground {
            val newBeacon = beacon.copy(visible = !beacon.visible)

            onIO {
                beaconService.add(newBeacon)
            }

            onMain {
                refresh()
            }
        }
    }

    private fun setGroupVisibility(group: BeaconGroup, visible: Boolean) {
        fragment.inBackground {
            Alerts.withLoading(fragment.requireContext(), fragment.getString(R.string.loading)) {
                val beacons = onIO {
                    beaconService.loader.getChildren(group.id, null).filterIsInstance<Beacon>()
                }

                onIO {
                    beacons
                        .asSequence()
                        .filter { it.visible != visible }
                        .forEach { beaconService.add(it.copy(visible = visible)) }
                }

                onMain {
                    refresh()
                }
            }
        }
    }

    private fun rename(group: BeaconGroup) {
        val command =
            RenameBeaconGroupCommand(fragment.requireContext(), fragment.viewLifecycleOwner.lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun navigateToNearestCellSignal() {
        fragment.inBackground {
            val command = NavigateToNearestCellSignal(fragment.requireContext())
            if (command.execute()) {
                onMain {
                    fragment.findNavController().navigate(R.id.action_navigation)
                }
            } else {
                onMain {
                    fragment.toast(fragment.getString(R.string.no_recorded_cell_signal_nearby))
                }
            }
        }
    }

    private fun handleBeaconGroupAction(group: BeaconGroup, action: BeaconGroupAction) {
        when (action) {
            BeaconGroupAction.Open -> {
                manager.open(group.id)
            }

            BeaconGroupAction.ShowAll -> setGroupVisibility(group, true)
            BeaconGroupAction.HideAll -> setGroupVisibility(group, false)
            BeaconGroupAction.Edit -> rename(group)
            BeaconGroupAction.Delete -> delete(group)
            BeaconGroupAction.Move -> move(group)
        }
    }

    private fun handleBeaconAction(beacon: Beacon, action: BeaconAction) {
        when (action) {
            BeaconAction.View -> viewBeacon(beacon.id)
            BeaconAction.Edit -> createBeacon(editingBeaconId = beacon.id)
            BeaconAction.Navigate -> navigate(beacon)
            BeaconAction.Delete -> delete(beacon)
            BeaconAction.Move -> move(beacon)
            BeaconAction.ToggleVisibility -> toggleVisibility(beacon)
            BeaconAction.Share -> BeaconSender(fragment).send(beacon)
        }
    }
}

@Composable
private fun BeaconListScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IBeacon>,
    isEmpty: Boolean,
    resetKey: Int,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    currentLocation: Coordinate,
    isCreateMenuOpen: Boolean,
    onSearchChange: (String) -> Unit,
    onToolbarAction: (BeaconToolbarAction) -> Unit,
    onCreateMenuOpenChange: (Boolean) -> Unit,
    onCreateAction: (BeaconCreateAction) -> Unit,
    onBeaconAction: (Beacon, BeaconAction) -> Unit,
    onGroupAction: (BeaconGroup, BeaconGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                BeaconListHeader(
                    title = title,
                    sortMenuText = sortMenuText,
                    onToolbarAction = onToolbarAction,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TrailSenseSpacing.lg)
                        .testTag("searchbox"),
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = TrailSenseSpacing.lg)
                ) {
                    BeaconList(
                        items = items,
                        resetKey = resetKey,
                        formatService = formatService,
                        baseDistanceUnits = baseDistanceUnits,
                        currentLocation = currentLocation,
                        onBeaconAction = onBeaconAction,
                        onGroupAction = onGroupAction,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isEmpty) {
                        Text(
                            text = stringResource(R.string.no_beacons),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("beacon_empty_text"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            BeaconCreateButton(
                isOpen = isCreateMenuOpen,
                onOpenChange = onCreateMenuOpenChange,
                onAction = onCreateAction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TrailSenseSpacing.lg)
            )
        }
    }
}

@Composable
private fun BeaconListHeader(
    title: String,
    sortMenuText: String,
    onToolbarAction: (BeaconToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.sm)
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("beacon_title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.testTag("beacon_list_menu_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_dots),
                    contentDescription = stringResource(R.string.sort)
                )
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                BeaconToolbarMenuItem(sortMenuText, { isMenuOpen = it }, onToolbarAction, BeaconToolbarAction.Sort)
                BeaconToolbarMenuItem(stringResource(R.string.export), { isMenuOpen = it }, onToolbarAction, BeaconToolbarAction.Export)
                BeaconToolbarMenuItem(
                    stringResource(R.string.navigate_to_nearest_cell_signal),
                    { isMenuOpen = it },
                    onToolbarAction,
                    BeaconToolbarAction.NavigateToNearestCellSignal
                )
                BeaconToolbarMenuItem(stringResource(R.string.delete), { isMenuOpen = it }, onToolbarAction, BeaconToolbarAction.Delete)
            }
        }
    }
}

@Composable
private fun BeaconToolbarMenuItem(
    label: String,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconToolbarAction) -> Unit,
    action: BeaconToolbarAction
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun BeaconCreateButton(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onAction: (BeaconCreateAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.End
        ) {
            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { onOpenChange(false) }
            ) {
                BeaconCreateMenuItem(R.string.qr_code, onAction, BeaconCreateAction.ImportQr)
                BeaconCreateMenuItem(R.string.import_gpx, onAction, BeaconCreateAction.ImportGpx)
                BeaconCreateMenuItem(R.string.group, onAction, BeaconCreateAction.CreateGroup)
                BeaconCreateMenuItem(R.string.beacon, onAction, BeaconCreateAction.CreateBeacon)
            }
        }
        FloatingActionButton(
            onClick = { onOpenChange(!isOpen) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("create_btn")
        ) {
            Icon(
                painter = painterResource(if (isOpen) R.drawable.ic_cancel else R.drawable.ic_add),
                contentDescription = stringResource(R.string.add)
            )
        }
    }
}

@Composable
private fun BeaconCreateMenuItem(
    label: Int,
    onAction: (BeaconCreateAction) -> Unit,
    action: BeaconCreateAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = { onAction(action) }
    )
}

@Composable
private fun BeaconList(
    items: List<IBeacon>,
    resetKey: Int,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    currentLocation: Coordinate,
    onBeaconAction: (Beacon, BeaconAction) -> Unit,
    onGroupAction: (BeaconGroup, BeaconGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("beacon_list")
    ) {
        items(items, key = { item ->
            if (item is BeaconGroup) {
                -item.id
            } else {
                item.id
            }
        }) { item ->
            when (item) {
                is Beacon -> BeaconRow(
                    beacon = item,
                    formatService = formatService,
                    baseDistanceUnits = baseDistanceUnits,
                    currentLocation = currentLocation,
                    onAction = { onBeaconAction(item, it) }
                )

                is BeaconGroup -> BeaconGroupRow(
                    group = item,
                    onAction = { onGroupAction(item, it) }
                )
            }
            TrailSenseDivider()
        }
    }
}

@Composable
private fun BeaconRow(
    beacon: Beacon,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    currentLocation: Coordinate,
    onAction: (BeaconAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val distance = remember(beacon.coordinate, currentLocation, baseDistanceUnits) {
        val meters = NavigationService().navigate(beacon.coordinate, currentLocation, 0f).distance
        Distance.meters(meters).convertTo(baseDistanceUnits).toRelativeDistance()
    }

    TrailSenseListItem(
        title = beacon.name,
        subtitle = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        ),
        icon = painterResource(getBeaconIcon(beacon, formatService)),
        iconTint = Color(getBeaconIconTint(beacon, formatService)),
        onClick = { onAction(BeaconAction.View) },
        trailing = {
            BeaconRowActions(
                beacon = beacon,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        },
        animateEntrance = false,
        modifier = Modifier
            .combinedClickable(
                onClick = { onAction(BeaconAction.View) },
                onLongClick = { onAction(BeaconAction.Navigate) }
            )
            .padding(horizontal = TrailSenseSpacing.lg)
    )
}

@Composable
private fun BeaconRowActions(
    beacon: Beacon,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconAction) -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        if (!beacon.temporary) {
            IconButton(
                onClick = { onAction(BeaconAction.ToggleVisibility) },
                modifier = Modifier.testTag("beacon_visibility_toggle")
            ) {
                Icon(
                    painter = painterResource(
                        if (beacon.visible) {
                            R.drawable.ic_visible
                        } else {
                            R.drawable.ic_not_visible
                        }
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(
                onClick = { onMenuOpenChange(true) },
                modifier = Modifier.testTag("beacon_item_menu")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_dots),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) }
            ) {
                BeaconMenuItem(R.string.navigate, onMenuOpenChange, onAction, BeaconAction.Navigate)
                BeaconMenuItem(R.string.share_ellipsis, onMenuOpenChange, onAction, BeaconAction.Share)
                if (!beacon.temporary) {
                    BeaconMenuItem(R.string.edit, onMenuOpenChange, onAction, BeaconAction.Edit)
                    BeaconMenuItem(R.string.move_to, onMenuOpenChange, onAction, BeaconAction.Move)
                    BeaconMenuItem(R.string.delete, onMenuOpenChange, onAction, BeaconAction.Delete)
                }
            }
        }
    }
}

@Composable
private fun BeaconMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconAction) -> Unit,
    action: BeaconAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

@Composable
private fun BeaconGroupRow(
    group: BeaconGroup,
    onAction: (BeaconGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    TrailSenseListItem(
        title = group.name,
        subtitle = stringResource(R.plurals.beacon_group_summary, group.count, group.count),
        icon = painterResource(R.drawable.ic_beacon_group),
        iconTint = Color(AppColor.Gray.color),
        onClick = { onAction(BeaconGroupAction.Open) },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("beacon_group_menu")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_dots),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    BeaconGroupMenuItem(R.string.show_all, { menuOpen = it }, onAction, BeaconGroupAction.ShowAll)
                    BeaconGroupMenuItem(R.string.hide_all, { menuOpen = it }, onAction, BeaconGroupAction.HideAll)
                    BeaconGroupMenuItem(R.string.rename, { menuOpen = it }, onAction, BeaconGroupAction.Edit)
                    BeaconGroupMenuItem(R.string.move_to, { menuOpen = it }, onAction, BeaconGroupAction.Move)
                    BeaconGroupMenuItem(R.string.delete, { menuOpen = it }, onAction, BeaconGroupAction.Delete)
                }
            }
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
    )
}

@Composable
private fun BeaconGroupMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (BeaconGroupAction) -> Unit,
    action: BeaconGroupAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

private fun getBeaconIcon(beacon: Beacon, formatService: FormatService): Int {
    if (beacon.owner == BeaconOwner.User) {
        return beacon.icon?.icon ?: R.drawable.bubble
    }

    return CellSignalUtils.getCellQualityImage(getCellSignalQuality(beacon, formatService))
}

private fun getBeaconIconTint(beacon: Beacon, formatService: FormatService): Int {
    if (beacon.owner == BeaconOwner.User) {
        return if (beacon.icon == null) {
            beacon.color
        } else {
            val background = Color(beacon.color)
            if (background.luminance() > 0.5f) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }

    val backgroundColor = CustomUiUtils.getQualityColor(getCellSignalQuality(beacon, formatService))
    return Colors.mostContrastingColor(android.graphics.Color.WHITE, android.graphics.Color.BLACK, backgroundColor)
}

private fun getCellSignalQuality(beacon: Beacon, formatService: FormatService): Quality {
    return when {
        beacon.name.contains(formatService.formatQuality(Quality.Poor)) -> Quality.Poor
        beacon.name.contains(formatService.formatQuality(Quality.Moderate)) -> Quality.Moderate
        beacon.name.contains(formatService.formatQuality(Quality.Good)) -> Quality.Good
        else -> Quality.Unknown
    }
}

private enum class BeaconToolbarAction {
    Sort,
    Export,
    NavigateToNearestCellSignal,
    Delete
}

private enum class BeaconCreateAction {
    ImportQr,
    ImportGpx,
    CreateGroup,
    CreateBeacon
}

@Preview(showBackground = true)
@Composable
private fun BeaconListScreenPreview() {
    TrailSenseTheme {
        BeaconListScreen(
            title = "Beacons",
            sortMenuText = "Sort by Closest",
            searchText = "",
            items = emptyList(),
            isEmpty = true,
            resetKey = 0,
            formatService = FormatService.getInstance(androidx.compose.ui.platform.LocalContext.current),
            baseDistanceUnits = DistanceUnits.Meters,
            currentLocation = Coordinate.zero,
            isCreateMenuOpen = false,
            onSearchChange = {},
            onToolbarAction = {},
            onCreateMenuOpenChange = {},
            onCreateAction = {},
            onBeaconAction = { _, _ -> },
            onGroupAction = { _, _ -> }
        )
    }
}
