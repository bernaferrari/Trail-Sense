package com.kylecorry.trail_sense.tools.paths.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
    fragment: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(fragment) { PathsController(fragment) }
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

private class PathsController(private val fragment: TrailSenseDestinationHost) {

    private val service by lazy {
        Tools.getService(
            fragment.requireContext(),
            PathsToolRegistration.SERVICE_BACKTRACK
        )
    }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val pathService by lazy {
        PathService.getInstance(fragment.requireContext())
    }

    private val gps by lazy {
        SensorService(fragment.requireContext()).getGPS()
    }

    private val gpxService by lazy {
        IOFactory().createGpxService(
            fragment.requireContext(),
            fragment.andromedaActivity as IntentResultRetriever
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

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)
        PathsScreen(
            title = title.ifBlank { stringResource(R.string.paths) },
            sortMenuText = sortMenuText.ifBlank {
                stringResource(R.string.sort_by, stringResource(R.string.most_recent))
            },
            searchText = searchText,
            items = pathItems,
            isEmpty = pathItems.isEmpty(),
            resetKey = pathListResetKey,
            backtrackState = backtrackState,
            backtrackFrequency = backtrackFrequency,
            formatService = FormatService.getInstance(fragment.requireContext()),
            baseDistanceUnits = prefs.baseDistanceUnits,
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
                duration = prefs.backtrackRecordFrequency,
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
            fragment.lifecycleScope,
            pathLoader,
            lastRoot,
            this::sortPaths
        )

        manager.onChange = { root, items, rootChanged ->
            pathItems = items
            if (rootChanged) {
                pathListResetKey++
            }
            title = (root as PathGroup?)?.name ?: fragment.getString(R.string.paths)
        }

        sort = prefs.navigation.pathSort
        sortMenuText = fragment.getString(R.string.sort_by, getSortString(sort))

        // TODO: See if it is possible to get notified of changes without loading all paths
        fragment.lifecycleScope.launch {
            pathService.getPaths().collect {
                manager.refresh()
            }
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!manager.up()) {
                    remove()
                    fragment.findNavController().navigateUp()
                }
            }
        }
        fragment.andromedaActivity.onBackPressedDispatcher.addCallback(
            fragment.lifecycleOwner,
            backPressedCallback!!
        )

        title = fragment.getString(R.string.paths)
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
            fragment.requireContext(),
            fragment.lifecycleScope
        ) { updateStatusBar() }.execute(frequency)
    }

    private fun toggleBacktrack() {
        val command = ToggleBacktrackCommand(fragment)
        command.execute()
    }

    private fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            fragment.requireContext(),
            fragment.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.navigation.pathSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.navigation.pathSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                sortMenuText = fragment.getString(R.string.sort_by, getSortString(sort))
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> fragment.getString(R.string.most_recent)
            PathSortMethod.Longest -> fragment.getString(R.string.longest)
            PathSortMethod.Shortest -> fragment.getString(R.string.shortest)
            PathSortMethod.Closest -> fragment.getString(R.string.closest)
            PathSortMethod.Name -> fragment.getString(R.string.name)
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
        fragment.inBackground {
            Alerts.withLoading(fragment.requireContext(), fragment.getString(R.string.loading)) {
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
            fragment.requireContext(),
            fragment.lifecycleOwner,
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
            fragment.requireContext(),
            fragment.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(
            fragment.requireContext(),
            fragment.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(
            fragment.requireContext(),
            fragment.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun showPath(path: Path) {
        val command = ViewPathCommand(fragment.findNavController())
        command.execute(path)
    }

    private fun showPath(id: Long) {
        val command = ViewPathCommand(fragment.findNavController())
        command.execute(id)
    }

    private fun importPaths() {
        val command = ImportPathsCommand(
            fragment.requireContext(),
            fragment.lifecycleOwner,
            gpxService,
            pathService,
            prefs.navigation
        )
        command.execute(manager.root?.id)
    }

    private fun exportPath(path: IPath?) {
        val command = ExportPathCommand(
            fragment.requireContext(),
            fragment.lifecycleOwner,
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
            fragment.requireContext(),
            fragment.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun merge(path: Path) {
        val command = MergePathCommand(
            fragment.requireContext(),
            fragment.lifecycleOwner,
            pathService
        )
        command.execute(path)
    }

    private fun createPath() {
        val command = CreatePathCommand(fragment.requireContext(), pathService, prefs.navigation)
        fragment.inBackground {
            command.execute(manager.root?.id)?.let {
                showPath(it)
            }
        }
    }

    // Groups
    private fun deleteGroup(group: PathGroup) {
        val command = DeletePathGroupGroupCommand(fragment.requireContext(), pathService)
        fragment.inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun renameGroup(group: PathGroup) {
        val command = RenamePathGroupGroupCommand(fragment.requireContext(), pathService)
        fragment.inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun createGroup() {
        val command = CreatePathGroupCommand(fragment.requireContext(), pathService)
        fragment.inBackground {
            command.execute(manager.root?.id)
            manager.refresh()
        }
    }

    private fun movePath(path: IPath) {
        val command = MoveIPathCommand(fragment.requireContext(), pathService)
        fragment.inBackground {
            val newGroup = command.execute(path)
            if (newGroup?.id != path.parentId) {
                fragment.toast(fragment.getString(R.string.moved_to, newGroup?.name ?: fragment.getString(R.string.no_group)))
                manager.refresh()
            }
        }
    }

    private fun onBacktrackChanged(data: Bundle): Boolean {
        updateStatusBar()
        return true
    }

}

@Composable
private fun PathsScreen(
    title: String,
    sortMenuText: String,
    searchText: String,
    items: List<IPath>,
    isEmpty: Boolean,
    resetKey: Int,
    backtrackState: FeatureState,
    backtrackFrequency: Duration?,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PathsHeader(
                title = title,
                sortMenuText = sortMenuText,
                onMenuAction = onMenuAction,
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
                PathsList(
                    items = items,
                    resetKey = resetKey,
                    formatService = formatService,
                    baseDistanceUnits = baseDistanceUnits,
                    onPathAction = onPathAction,
                    onGroupAction = onGroupAction,
                    modifier = Modifier.fillMaxSize()
                )
                if (isEmpty) {
                    Text(
                        text = stringResource(R.string.no_paths),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("waypoints_empty_text"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                PathsCreateButton(
                    onImportPath = onImportPath,
                    onCreateGroup = onCreateGroup,
                    onCreatePath = onCreatePath,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(TrailSenseSpacing.lg)
                )
            }
            BacktrackPlayBar(
                state = backtrackState,
                frequency = backtrackFrequency,
                formatService = formatService,
                onSubtitleClick = onBacktrackFrequencyClick,
                onPlayClick = onBacktrackToggleClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PathsHeader(
    title: String,
    sortMenuText: String,
    onMenuAction: (PathsToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.sm)
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("paths_title"),
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
                modifier = Modifier.testTag("paths_menu_btn")
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
                DropdownMenuItem(
                    text = { Text(sortMenuText) },
                    onClick = {
                        isMenuOpen = false
                        onMenuAction(PathsToolbarAction.Sort)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export)) },
                    onClick = {
                        isMenuOpen = false
                        onMenuAction(PathsToolbarAction.Export)
                    }
                )
            }
        }
    }
}

@Composable
private fun PathsCreateButton(
    onImportPath: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreatePath: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.End
        ) {
            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { isOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.import_gpx)) },
                    onClick = {
                        isOpen = false
                        onImportPath()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.group)) },
                    onClick = {
                        isOpen = false
                        onCreateGroup()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.path)) },
                    onClick = {
                        isOpen = false
                        onCreatePath()
                    }
                )
            }
        }
        FloatingActionButton(
            onClick = { isOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("add_btn")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.add)
            )
        }
    }
}

@Composable
private fun BacktrackPlayBar(
    state: FeatureState,
    frequency: Duration?,
    formatService: FormatService,
    onSubtitleClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = state == FeatureState.On
    val subtitle = stringResource(if (isOn) R.string.on else R.string.off) + (frequency?.let {
        " ${stringResource(R.string.dash)} ${
            formatService.formatDuration(
                it,
                short = true,
                includeSeconds = true
            )
        }"
    } ?: "")

    Surface(
        modifier = modifier.testTag("backtrack_play_bar"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tool_backtrack),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.lg)
                    .testTag("play_bar_title")
            ) {
                Text(
                    text = stringResource(R.string.backtrack),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.clickable(onClick = onSubtitleClick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.testTag("play_btn")
            ) {
                Icon(
                    painter = painterResource(
                        if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24
                    ),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun PathsList(
    items: List<IPath>,
    resetKey: Int,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onPathAction: (Path, PathAction) -> Unit,
    onGroupAction: (PathGroup, PathGroupAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("paths_list")
    ) {
        items(items, key = { item ->
            if (item is PathGroup) {
                -item.id
            } else {
                item.id
            }
        }) { item ->
            when (item) {
                is Path -> PathRow(
                    path = item,
                    formatService = formatService,
                    baseDistanceUnits = baseDistanceUnits,
                    onAction = { onPathAction(item, it) }
                )

                is PathGroup -> PathGroupRow(
                    group = item,
                    onAction = { onGroupAction(item, it) }
                )
            }
            TrailSenseDivider()
        }
    }
}

@Composable
private fun PathRow(
    path: Path,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onAction: (PathAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val nameFactory = PathNameFactory(LocalContext.current)
    val title = nameFactory.getName(path)
    val distance = path.metadata.distance.convertTo(baseDistanceUnits).toRelativeDistance()
    val distanceString = formatService.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
    val subtitle = if (path.temporary) {
        "${stringResource(R.string.temporary)}    $distanceString"
    } else {
        distanceString
    }

    TrailSenseListItem(
        title = title,
        subtitle = subtitle,
        icon = painterResource(getPathIcon(path)),
        iconTint = androidx.compose.ui.graphics.Color(path.style.color),
        onClick = { onAction(PathAction.Show) },
        trailing = {
            PathRowActions(
                path = path,
                title = title,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onAction = onAction
            )
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
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
    Box {
        IconButton(
            onClick = { onAction(PathAction.ToggleVisibility) },
            modifier = Modifier.testTag("path_visibility_toggle_$title")
        ) {
            Icon(
                painter = painterResource(
                    if (path.style.visible) {
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
            modifier = Modifier.testTag("path_item_menu_$title")
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
    onAction: (PathGroupAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = group.count ?: 0

    TrailSenseListItem(
        title = group.name,
        subtitle = stringResource(R.plurals.path_group_summary, count, count),
        icon = painterResource(R.drawable.ic_path_group),
        iconTint = androidx.compose.ui.graphics.Color(AppColor.Gray.color),
        onClick = { onAction(PathGroupAction.Open) },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("path_group_menu_${group.name}")
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
                    PathGroupMenuItem(R.string.show_all, { menuOpen = it }, onAction, PathGroupAction.ShowAll)
                    PathGroupMenuItem(R.string.hide_all, { menuOpen = it }, onAction, PathGroupAction.HideAll)
                    PathGroupMenuItem(R.string.rename, { menuOpen = it }, onAction, PathGroupAction.Rename)
                    PathGroupMenuItem(R.string.export, { menuOpen = it }, onAction, PathGroupAction.Export)
                    PathGroupMenuItem(R.string.delete, { menuOpen = it }, onAction, PathGroupAction.Delete)
                    PathGroupMenuItem(R.string.move_to, { menuOpen = it }, onAction, PathGroupAction.Move)
                }
            }
        },
        animateEntrance = false,
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
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

private fun getPathIcon(path: Path): Int {
    if (!path.style.visible) {
        return R.drawable.ic_not_visible
    }

    return when (path.style.line) {
        LineStyle.Solid -> R.drawable.path_solid
        LineStyle.Dotted -> R.drawable.path_dotted
        LineStyle.Arrow -> R.drawable.path_arrow
        LineStyle.Dashed -> R.drawable.path_dashed
        LineStyle.Square -> R.drawable.path_square
        LineStyle.Diamond -> R.drawable.path_diamond
        LineStyle.Cross -> R.drawable.path_cross
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
            sortMenuText = "Sort by Most recent",
            searchText = "",
            items = emptyList(),
            isEmpty = true,
            resetKey = 0,
            backtrackState = FeatureState.Off,
            backtrackFrequency = Duration.ofMinutes(15),
            formatService = FormatService.getInstance(LocalContext.current),
            baseDistanceUnits = DistanceUnits.Meters,
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
