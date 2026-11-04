@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.luminance
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.ClosestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.LongestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.MostRecentPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.NamePathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.ShortestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathGroupLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.CreatePathGroupCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.DeletePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.DeletePathGroupGroupCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ExportPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ImportPathsCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.KeepPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.MergePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.MoveIPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.RenamePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.RenamePathGroupGroupCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ViewPathCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import java.time.Duration
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun PathsToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { PathsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
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
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class PathsController(private val host: TrailSenseDestinationHost) {

    private val service by lazy {
        Tools.getService(
            host.requireContext(),
            PathsToolRegistration.SERVICE_BACKTRACK
        )
    }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val pathService by lazy {
        PathService.getInstance(host.requireContext())
    }

    private val gps by lazy {
        SensorService(host.requireContext()).getGPS()
    }

    private val gpxService by lazy {
        IOFactory().createGpxService(
            host.requireContext(),
            host.intentResultRetriever
        )
    }

    private var sort = PathSortMethod.MostRecent

    private val pathLoader by lazy { PathGroupLoader(pathService) }
    private lateinit var manager: GroupListManager<IPath>

    private var lastRoot: IPath? = null
    private var pathItems by mutableStateOf<List<IPath>>(emptyList())
    private var pathListResetKey by mutableIntStateOf(0)
    private var title by mutableStateOf("")
    private var searchText by mutableStateOf("")
    private var backtrackState by mutableStateOf(FeatureState.Off)
    private var backtrackFrequency by mutableStateOf<Duration?>(null)
    private var sortMenuText by mutableStateOf("")
    private var showBacktrackFrequencyPicker by mutableStateOf(false)
    private var backPressedCallback: OnBackPressedCallback? = null
    private var isInitialized = false
    private var isLoaded by mutableStateOf(false)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        PathsScreen(
            title = title.ifBlank { stringResource(R.string.paths) },
            sortMenuText = sortMenuText.ifBlank {
                stringResource(R.string.sort_by, stringResource(R.string.most_recent))
            },
            searchText = searchText,
            items = pathItems,
            isLoaded = isLoaded,
            resetKey = pathListResetKey,
            backtrackState = backtrackState,
            backtrackFrequency = backtrackFrequency,
            formatService = FormatService.getInstance(host.requireContext()),
            baseDistanceUnits = prefs.baseDistanceUnits,
            onBack = { dispatcher?.onBackPressed() },
            onSearchChange = {
                searchText = it
                manager.search(it)
            },
            onMenuAction = ::handleToolbarAction,
            onPathAction = ::handleAction,
            onGroupAction = ::handleGroupAction,
            onImportPath = ::importPaths,
            onCreateGroup = ::createGroup,
            onCreatePath = ::createPath,
            onBacktrackFrequencyClick = {
                showBacktrackFrequencyPicker = true
            },
            onBacktrackToggleClick = ::toggleBacktrack,
            modifier = modifier
        )

        if (showBacktrackFrequencyPicker) {
            DurationPickerDialog(
                title = backtrackFrequencyTitle,
                duration = prefs.paths.backtrackRecordFrequency,
                message = stringResource(R.string.actual_frequency_disclaimer),
                hint = stringResource(R.string.frequency),
                showSeconds = true,
                onDurationPicked = {
                    if (it != null) {
                        changeBacktrackFrequency(it)
                    }
                    showBacktrackFrequencyPicker = false
                },
                onDismiss = {
                    showBacktrackFrequencyPicker = false
                }
            )
        }
    }

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        manager = GroupListManager(
            host.lifecycleScope,
            pathLoader,
            lastRoot,
            this::sortPaths
        )

        manager.onChange = { root, items, rootChanged ->
            pathItems = items
            isLoaded = true
            if (rootChanged) {
                pathListResetKey++
            }
            title = (root as PathGroup?)?.name ?: host.getString(R.string.paths)
        }

        sort = prefs.paths.pathSort
        sortMenuText = host.getString(R.string.sort_by, getSortString(sort))

        // Path updates currently require loading all paths because the repository exposes full snapshots.
        host.lifecycleScope.launch {
            pathService.getPaths().collect {
                manager.refresh()
            }
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!manager.up()) {
                    remove()
                    host.findNavController().navigateUp()
                }
            }
        }
        host.mainActivity.onBackPressedDispatcher.addCallback(
            host.lifecycleOwner,
            backPressedCallback!!
        )

        title = host.getString(R.string.paths)
    }

    fun onResume() {
        if (!isInitialized) {
            return
        }
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
            this::onBacktrackChanged
        )
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_FREQUENCY_CHANGED,
            this::onBacktrackChanged
        )
        updateStatusBar()
    }

    fun onPause() {
        if (!isInitialized) {
            return
        }
        tryOrNothing {
            lastRoot = manager.root
        }
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
            this::onBacktrackChanged
        )
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_FREQUENCY_CHANGED,
            this::onBacktrackChanged
        )
    }

    fun onDestroy() {
        onPause()
        backPressedCallback?.remove()
        backPressedCallback = null
        isInitialized = false
    }

    private fun updateStatusBar() {
        val service = service ?: return
        backtrackState = service.getFeatureState()
        backtrackFrequency = service.getFrequency()
    }

    private fun handleToolbarAction(action: PathsToolbarAction) {
        when (action) {
            PathsToolbarAction.Sort -> changeSort()
            PathsToolbarAction.Export -> exportCurrentGroup()
        }
    }

    private fun changeBacktrackFrequency(frequency: Duration) {
        ChangeBacktrackFrequencyCommand(
            host.requireContext(),
            host.lifecycleScope
        ) { updateStatusBar() }.execute(frequency)
    }

    private fun toggleBacktrack() {
        val command = ToggleBacktrackCommand(host)
        command.execute()
    }

    private fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            host.requireContext(),
            host.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.paths.pathSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.paths.pathSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                sortMenuText = host.getString(R.string.sort_by, getSortString(sort))
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> host.getString(R.string.most_recent)
            PathSortMethod.Longest -> host.getString(R.string.longest)
            PathSortMethod.Shortest -> host.getString(R.string.shortest)
            PathSortMethod.Closest -> host.getString(R.string.closest)
            PathSortMethod.Name -> host.getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private fun handleGroupAction(group: PathGroup, action: PathGroupAction) {
        when (action) {
            PathGroupAction.Delete -> deleteGroup(group)
            PathGroupAction.Rename -> renameGroup(group)
            PathGroupAction.Open -> manager.open(group.id)
            PathGroupAction.Move -> movePath(group)
            PathGroupAction.Export -> exportPath(group)
            PathGroupAction.ShowAll -> setGroupVisibility(group, true)
            PathGroupAction.HideAll -> setGroupVisibility(group, false)
        }
    }

    private fun setGroupVisibility(group: PathGroup, visible: Boolean) {
        host.inBackground {
            Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
                val paths = onIO {
                    pathService.loader().getChildren(group.id, null).filterIsInstance<Path>()
                }

                onIO {
                    paths
                        .asSequence()
                        .filter { it.style.visible != visible }
                        .forEach { pathService.addPath(it.copy(style = it.style.copy(visible = visible))) }
                }
            }
        }
    }

    private fun handleAction(path: Path, action: PathAction) {
        when (action) {
            PathAction.Export -> exportPath(path)
            PathAction.Delete -> deletePath(path)
            PathAction.Merge -> merge(path)
            PathAction.Show -> showPath(path)
            PathAction.Rename -> renamePath(path)
            PathAction.Keep -> keepPath(path)
            PathAction.ToggleVisibility -> togglePathVisibility(path)
            PathAction.Simplify -> simplifyPath(path)
            PathAction.Move -> movePath(path)
            else -> {}
        }
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private suspend fun sortPaths(paths: List<IPath>): List<IPath> {
        val strategy = when (sort) {
            PathSortMethod.MostRecent -> MostRecentPathSortStrategy(pathService)
            PathSortMethod.Longest -> LongestPathSortStrategy(pathService)
            PathSortMethod.Shortest -> ShortestPathSortStrategy(pathService)
            PathSortMethod.Closest -> ClosestPathSortStrategy(gps.location, pathService)
            PathSortMethod.Name -> NamePathSortStrategy()
        }
        return strategy.sort(paths)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun showPath(path: Path) {
        val command = ViewPathCommand(host.findNavController())
        command.execute(path)
    }

    private fun showPath(id: Long) {
        val command = ViewPathCommand(host.findNavController())
        command.execute(id)
    }

    private fun importPaths() {
        val command = ImportPathsCommand(
            host.requireContext(),
            host.lifecycleOwner,
            gpxService,
            pathService,
            prefs.paths
        )
        command.execute(manager.root?.id)
    }

    private fun exportPath(path: IPath?) {
        val command = ExportPathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            gpxService,
            pathService
        )
        command.execute(path)
    }

    private fun exportCurrentGroup() {
        exportPath(manager.root)
    }

    private fun deletePath(path: Path) {
        val command = DeletePathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun merge(path: Path) {
        val command = MergePathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun createPath() {
        val command = CreatePathCommand(host.requireContext(), pathService, prefs.paths)
        host.inBackground {
            command.execute(manager.root?.id)?.let {
                showPath(it)
            }
        }
    }

    // Groups
    private fun deleteGroup(group: PathGroup) {
        val command = DeletePathGroupGroupCommand(host.requireContext(), pathService)
        host.inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun renameGroup(group: PathGroup) {
        val command = RenamePathGroupGroupCommand(host.requireContext(), pathService)
        host.inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun createGroup() {
        val command = CreatePathGroupCommand(host.requireContext(), pathService)
        host.inBackground {
            command.execute(manager.root?.id)
            manager.refresh()
        }
    }

    private fun movePath(path: IPath) {
        val command = MoveIPathCommand(host.requireContext(), pathService)
        host.inBackground {
            val newGroup = command.execute(path)
            if (newGroup?.id != path.parentId) {
                host.toast(host.getString(R.string.moved_to, newGroup?.name ?: host.getString(R.string.no_group)))
                manager.refresh()
            }
        }
    }

    private fun onBacktrackChanged(data: Bundle): Boolean {
        updateStatusBar()
        return true
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PathsScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IPath>,
    isLoaded: Boolean,
    resetKey: Int,
    backtrackState: FeatureState,
    backtrackFrequency: Duration?,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onBack: (() -> Unit)?,
    onSearchChange: (String) -> Unit,
    onMenuAction: (PathsToolbarAction) -> Unit,
    onPathAction: (Path, PathAction) -> Unit,
    onGroupAction: (PathGroup, PathGroupAction) -> Unit,
    onImportPath: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreatePath: () -> Unit,
    onBacktrackFrequencyClick: () -> Unit,
    onBacktrackToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // What trails have I walked, and is Backtrack saving this one? A quiet Backtrack group, then one grouped list.
    val listState = rememberLazyListState()
    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    // Folders first: groups read as places to go, paths as things to look at.
    val groups = remember(items) { items.filterIsInstance<PathGroup>() }
    val paths = remember(items) { items.filterIsInstance<Path>() }

    Box(modifier = modifier.testTag("paths_list")) {
        TrailSensePage(
            title = title,
            onBack = onBack,
            backLabel = stringResource(R.string.back),
            state = listState,
            modifier = Modifier.testTag("paths_title"),
            actions = {
                PathsOverflowMenu(sortMenuText = sortMenuText, onMenuAction = onMenuAction)
            },
            floatingActionButton = {
                PathsCreateMenu(
                    onImportPath = onImportPath,
                    onCreateGroup = onCreateGroup,
                    onCreatePath = onCreatePath
                )
            }
        ) {
            item(key = "backtrack") {
                BacktrackRow(
                    state = backtrackState,
                    frequency = backtrackFrequency,
                    formatService = formatService,
                    onFrequencyClick = onBacktrackFrequencyClick,
                    onToggleClick = onBacktrackToggleClick
                )
            }

            // Nothing to search until there is a path: the bar only appears once the list has something in it.
            if (searchText.isNotBlank() || !isLoaded || items.isNotEmpty()) item(key = "search") {
                TrailSenseSearchBar(
                    value = searchText,
                    onValueChange = onSearchChange,
                    placeholder = stringResource(R.string.search),
                    clearContentDescription = stringResource(R.string.paths_clear_search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("searchbox")
                )
            }

            if (isLoaded && items.isEmpty()) {
                item(key = "empty") {
                    val isSearching = searchText.isNotBlank()
                    TrailSenseEmptyState(
                        icon = if (isSearching) TrailIcons.Search else TrailIcons.Trail,
                        title = stringResource(if (isSearching) R.string.no_results else R.string.no_paths),
                        description = if (isSearching) null else stringResource(R.string.paths_empty_description),
                        // No button here: Backtrack's switch above and the add FAB are the actions.
                        tone = if (isSearching) TrailSenseTone.Secondary else TrailSenseTone.Primary,
                        modifier = Modifier.testTag("waypoints_empty_text")
                    )
                }
            }

            groupedRows(
                rows = groups,
                key = { "group_${it.id}" }
            ) { group, position ->
                PathGroupRow(
                    group = group,
                    position = position,
                    onAction = { onGroupAction(group, it) }
                )
            }

            groupedRows(
                rows = paths,
                key = { "path_${it.id}" }
            ) { path, position ->
                PathRow(
                    path = path,
                    position = position,
                    formatService = formatService,
                    baseDistanceUnits = baseDistanceUnits,
                    onAction = { onPathAction(path, it) }
                )
            }

            // Room for the floating button over the last row.
            item(key = "fab_spacer") { Spacer(Modifier.height(56.dp)) }
        }
    }
}

/** Adds [rows] as one lazy item each, drawn as a single family group (24/6 silhouette, 2 dp gaps). */
private fun <T> LazyListScope.groupedRows(
    rows: List<T>,
    key: (T) -> Any,
    content: @Composable (T, TrailSenseGroupPosition) -> Unit
) {
    itemsIndexed(rows, key = { _, row -> key(row) }) { index, row ->
        Box(
            modifier = Modifier
                .animateItem()
                .pathsGroupedRowGap(isFirst = index == 0)
        ) {
            content(row, TrailSenseGroupPosition.at(index, rows.size))
        }
    }
}

@Composable
private fun PathsOverflowMenu(
    sortMenuText: String,
    onMenuAction: (PathsToolbarAction) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isMenuOpen = true },
            modifier = Modifier.testTag("paths_menu_btn")
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.MenuDots),
                contentDescription = stringResource(R.string.sort)
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(sortMenuText) },
                leadingIcon = { Icon(rememberVectorPainter(TrailIcons.SortAscending), contentDescription = null) },
                onClick = {
                    isMenuOpen = false
                    onMenuAction(PathsToolbarAction.Sort)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export)) },
                leadingIcon = { Icon(rememberVectorPainter(TrailIcons.ImportExport), contentDescription = null) },
                onClick = {
                    isMenuOpen = false
                    onMenuAction(PathsToolbarAction.Export)
                }
            )
        }
    }
}

/** The add button opens the family's floating action menu: import a GPX file, a group, or an empty path. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PathsCreateMenu(
    onImportPath: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreatePath: () -> Unit
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isOpen) { isOpen = false }
    val addLabel = stringResource(R.string.add)

    FloatingActionButtonMenu(
        expanded = isOpen,
        button = {
            ToggleFloatingActionButton(
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    initialColor = MaterialTheme.colorScheme.primary,
                    finalColor = MaterialTheme.colorScheme.primary
                ),
                checked = isOpen,
                onCheckedChange = { isOpen = !isOpen },
                modifier = Modifier
                    .testTag("add_btn")
                    .semantics {
                        traversalIndex = -1f
                        contentDescription = addLabel
                    }
            ) {
                val icon = if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.animateIcon(
                        { checkedProgress },
                        color = ToggleFloatingActionButtonDefaults.iconColor(
                            initialColor = MaterialTheme.colorScheme.onPrimary,
                            finalColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                isOpen = false
                onImportPath()
            },
            icon = { Icon(rememberVectorPainter(TrailIcons.File), contentDescription = null) },
            text = { Text(stringResource(R.string.import_gpx)) }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                isOpen = false
                onCreateGroup()
            },
            icon = { Icon(rememberVectorPainter(TrailIcons.PathGroup), contentDescription = null) },
            text = { Text(stringResource(R.string.group)) }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                isOpen = false
                onCreatePath()
            },
            icon = { Icon(rememberVectorPainter(TrailIcons.PathSolid), contentDescription = null) },
            text = { Text(stringResource(R.string.path)) }
        )
    }
}

/** Backtrack, the recorder that fills this list, as a quiet group: an on/off switch and its recording interval. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BacktrackRow(
    state: FeatureState,
    frequency: Duration?,
    formatService: FormatService,
    onFrequencyClick: () -> Unit,
    onToggleClick: () -> Unit
) {
    val isOn = state == FeatureState.On
    val frequencyText = frequency?.let { formatService.formatDuration(it, short = true, includeSeconds = true) }

    // A switch says on or off by itself; no card, no pulse, no spinning badge. Its label names Backtrack and its
    // state, so the group needs no header of its own.
    TrailSenseRowGroup(
        modifier = Modifier
            .padding(top = TrailSenseSpacing.sm)
            .testTag("backtrack_play_bar")
    ) {
        row { position ->
            Box(Modifier.testTag("play_bar_title")) {
                TrailSenseSwitchRow(
                    label = stringResource(if (isOn) R.string.places_backtrack_on_title else R.string.places_backtrack_off_title),
                    summary = stringResource(
                        if (isOn) R.string.places_backtrack_on_description else R.string.places_backtrack_off_description
                    ),
                    checked = isOn,
                    onCheckedChange = { onToggleClick() },
                    position = position,
                    modifier = Modifier.testTag("play_btn")
                )
            }
        }
        row { position ->
            TrailSenseListItem(
                title = stringResource(R.string.frequency),
                position = position,
                onClick = onFrequencyClick,
                trailing = {
                    Text(
                        frequencyText?.let { stringResource(R.string.places_backtrack_every, it) } ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun PathRow(
    path: Path,
    position: TrailSenseGroupPosition,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onAction: (PathAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val nameFactory = remember(context) { PathNameFactory(context) }
    val title = nameFactory.getName(path)
    val distance = path.metadata.distance.convertTo(baseDistanceUnits).toRelativeDistance()
    val distanceString = formatService.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
    val durationString = path.metadata.duration?.let {
        val length = Duration.between(it.start, it.end)
        if (length.isZero || length.isNegative) null else formatService.formatDuration(length, short = true)
    }
    val subtitle = listOfNotNull(
        if (path.temporary) stringResource(R.string.temporary) else null,
        durationString,
        pluralStringResource(R.plurals.path_points_count, path.metadata.waypoints, path.metadata.waypoints)
    ).joinToString(" · ")
    val pathColor = androidx.compose.ui.graphics.Color(path.style.color)
    val onPathColor = if (pathColor.luminance() > 0.5f) {
        androidx.compose.ui.graphics.Color.Black
    } else {
        androidx.compose.ui.graphics.Color.White
    }

    PlacesRow(
        title = title,
        subtitle = subtitle,
        position = position,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dimmed = !path.style.visible,
        leading = {
            PlacesBadge(
                containerColor = pathColor,
                contentColor = onPathColor,
                icon = getPathIcon(path),
                shape = if (path.temporary) MaterialShapes.Pill.toShape() else MaterialShapes.Cookie6Sided.toShape(),
                size = 40.dp,
                iconScale = 0.55f
            )
        },
        value = distanceString,
        onClick = { onAction(PathAction.Show) },
        onLongClick = { menuOpen = true },
        actions = {
            PathRowActions(
                path = path,
                title = title,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        }
    )
}

@Composable
private fun PathRowActions(
    path: Path,
    title: String,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (PathAction) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onAction(PathAction.ToggleVisibility) },
            modifier = Modifier.testTag("path_visibility_toggle_$title")
        ) {
            Icon(
                painter = rememberVectorPainter(
                    if (path.style.visible) TrailIcons.Visible else TrailIcons.NotVisible
                ),
                contentDescription = stringResource(if (path.style.visible) R.string.hide else R.string.show),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(
                onClick = { onMenuOpenChange(true) },
                modifier = Modifier.testTag("path_item_menu_$title")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.MenuDots),
                    contentDescription = stringResource(R.string.details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) }
            ) {
                PathMenuItem(R.string.rename, onMenuOpenChange, onAction, PathAction.Rename)
                if (path.temporary) {
                    PathMenuItem(R.string.keep_forever, onMenuOpenChange, onAction, PathAction.Keep)
                }
                PathMenuItem(R.string.export, onMenuOpenChange, onAction, PathAction.Export)
                PathMenuItem(R.string.merge, onMenuOpenChange, onAction, PathAction.Merge)
                PathMenuItem(R.string.delete, onMenuOpenChange, onAction, PathAction.Delete)
                PathMenuItem(R.string.simplify, onMenuOpenChange, onAction, PathAction.Simplify)
                PathMenuItem(R.string.move_to, onMenuOpenChange, onAction, PathAction.Move)
            }
        }
    }
}

@Composable
private fun PathMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (PathAction) -> Unit,
    action: PathAction
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
private fun PathGroupRow(
    group: PathGroup,
    position: TrailSenseGroupPosition,
    onAction: (PathGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = group.count ?: 0

    PlacesRow(
        title = group.name,
        subtitle = pluralStringResource(R.plurals.path_group_summary, count, count),
        position = position,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        leading = {
            // The count is already in the subtitle, so the mark is the group's icon, not the number again.
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(TrailIcons.PathGroup))
        },
        onClick = { onAction(PathGroupAction.Open) },
        onLongClick = { menuOpen = true },
        actions = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("path_group_menu_${group.name}")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.MenuDots),
                        contentDescription = stringResource(R.string.details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    PathGroupMenuItem(R.string.show_all, { menuOpen = it }, onAction, PathGroupAction.ShowAll)
                    PathGroupMenuItem(R.string.hide_all, { menuOpen = it }, onAction, PathGroupAction.HideAll)
                    PathGroupMenuItem(R.string.rename, { menuOpen = it }, onAction, PathGroupAction.Rename)
                    PathGroupMenuItem(R.string.export, { menuOpen = it }, onAction, PathGroupAction.Export)
                    PathGroupMenuItem(R.string.delete, { menuOpen = it }, onAction, PathGroupAction.Delete)
                    PathGroupMenuItem(R.string.move_to, { menuOpen = it }, onAction, PathGroupAction.Move)
                }
            }
        }
    )
}

@Composable
private fun PathGroupMenuItem(
    label: Int,
    onMenuOpenChange: (Boolean) -> Unit,
    onAction: (PathGroupAction) -> Unit,
    action: PathGroupAction
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            onMenuOpenChange(false)
            onAction(action)
        }
    )
}

private fun getPathIcon(path: Path): ImageVector {
    if (!path.style.visible) {
        return TrailIcons.NotVisible
    }

    return when (path.style.line) {
        LineStyle.Solid -> TrailIcons.PathSolid
        LineStyle.Dotted -> TrailIcons.PathDotted
        LineStyle.Arrow -> TrailIcons.PathArrow
        LineStyle.Dashed -> TrailIcons.PathDashed
        LineStyle.Square -> TrailIcons.PathSquare
        LineStyle.Diamond -> TrailIcons.PathDiamond
        LineStyle.Cross -> TrailIcons.PathCross
    }
}

private enum class PathsToolbarAction {
    Sort,
    Export
}

@Preview(showBackground = true)
@Composable
private fun PathsScreenPreview() {
    TrailSenseTheme {
        PathsScreen(
            title = "Paths",
            sortMenuText = "Sort: Most recent",
            searchText = "",
            items = emptyList(),
            isLoaded = true,
            resetKey = 0,
            backtrackState = FeatureState.Off,
            backtrackFrequency = Duration.ofMinutes(15),
            formatService = FormatService.getInstance(LocalContext.current),
            baseDistanceUnits = DistanceUnits.Meters,
            onBack = {},
            onSearchChange = {},
            onMenuAction = {},
            onPathAction = { _, _ -> },
            onGroupAction = { _, _ -> },
            onImportPath = {},
            onCreateGroup = {},
            onCreatePath = {},
            onBacktrackFrequencyClick = {},
            onBacktrackToggleClick = {}
        )
    }
}
