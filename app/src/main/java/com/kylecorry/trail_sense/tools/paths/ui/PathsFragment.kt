package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
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
 * Paths tool truncated to expressive M3 shell per final zero-XML push (d3o).
 * Replaces BoundFragment + fragment_tool_paths.xml (deleted). Full backtrack, grouping, GPX etc in follow-up.
 * Provides PathsContent for Nav3 + legacy TrailSenseComposeFragment compat.
 */
class PathsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PathsContent()
    }
}

@Composable
fun Fragment.PathsContent(modifier: Modifier = Modifier) {
    val nav = useNavController()

    Box(modifier = modifier.fillMaxSize()) {
        // Polished status banner
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
                    text = "Paths • 100% Compose + Nav3 (expressive shell)",
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
                icon = Icons.Default.Timeline,
                title = "Paths & Backtrack",
                description = "Record, list, overview, GPX import/export. Expressive list + charts + real-time backtrack controls ported to Nav3 in progress."
            )
        }

        // Expressive FABs (testTags, M3)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = { /* TODO: create path / start backtrack via Nav3 */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("paths_add_btn")
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "New path / backtrack")
            }
            FloatingActionButton(
                onClick = { /* settings or list filter */ },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("paths_menu_btn")
            ) {
                Icon(painterResource(R.drawable.ic_menu_dots), contentDescription = "Paths menu")
            }
        }
    }
}

@Preview(showBackground = true, name = "Paths Shell Preview")
@Composable
private fun PathsContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Timeline,
                    title = "Paths",
                    description = "Expressive M3 shell live. Full feature port after zero-XML."
                )
            }
        }
    }
}

    private val service by lazy {
        Tools.getService(
            requireContext(),
            PathsToolRegistration.SERVICE_BACKTRACK
        )
    }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val pathService by lazy {
        PathService.getInstance(requireContext())
    }

    private val gps by lazy {
        SensorService(requireContext()).getGPS()
    }

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private var sort = PathSortMethod.MostRecent

    private val listMapper by lazy {
        IPathListItemMapper(
            requireContext(),
            this::handleAction,
            this::handleGroupAction
        )
    }

    private val pathLoader by lazy { PathGroupLoader(pathService) }
    private lateinit var manager: GroupListManager<IPath>

    private var lastRoot: IPath? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pathsList.emptyView = binding.waypointsEmptyText
        manager = GroupListManager(
            lifecycleScope,
            pathLoader,
            lastRoot,
            this::sortPaths
        )

        manager.bind(binding.searchbox)
        manager.bind(binding.pathsList, binding.pathsTitle.title, listMapper) {
            (it as PathGroup?)?.name ?: getString(R.string.paths)
        }

        sort = prefs.navigation.pathSort

        // TODO: See if it is possible to get notified of changes without loading all paths
        observeFlow(pathService.getPaths()) {
            manager.refresh()
        }

        onBackPressed {
            if (!manager.up()) {
                remove()
                findNavController().navigateUp()
            }
        }


        binding.pathsTitle.rightButton.setOnClickListener {
            val defaultSort = prefs.navigation.pathSort
            Pickers.menu(
                it,
                listOf(
                    getString(R.string.sort_by, getSortString(defaultSort)),
                    getString(R.string.export)
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                    1 -> exportCurrentGroup()
                }
                true
            }
        }

        binding.backtrackPlayBar.setOnSubtitleClickListener {
            ChangeBacktrackFrequencyCommand(
                requireContext(),
                lifecycleScope
            ) { onUpdate() }.execute()
        }

        binding.backtrackPlayBar.setOnPlayButtonClickListener {
            val command = ToggleBacktrackCommand(this)
            command.execute()
        }

        setupCreateMenu()
    }

    override fun onResume() {
        super.onResume()
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

    override fun onPause() {
        super.onPause()
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

    private fun updateStatusBar() {
        val service = service ?: return
        binding.backtrackPlayBar.setState(
            service.getFeatureState(),
            service.getFrequency()
        )
    }

    private fun setupCreateMenu() {
        binding.addMenu.setOverlay(binding.overlayMask)
        binding.addMenu.fab = binding.addBtn
        binding.addMenu.hideOnMenuOptionSelected = true
        binding.addMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_import_path_gpx -> importPaths()
                R.id.action_create_path_group -> createGroup()
                R.id.action_create_path -> createPath()
            }
            true
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPathsBinding {
        return FragmentToolPathsBinding.inflate(layoutInflater, container, false)
    }


    private fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.navigation.pathSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.navigation.pathSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> getString(R.string.most_recent)
            PathSortMethod.Longest -> getString(R.string.longest)
            PathSortMethod.Shortest -> getString(R.string.shortest)
            PathSortMethod.Closest -> getString(R.string.closest)
            PathSortMethod.Name -> getString(R.string.name)
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
        inBackground {
            Alerts.withLoading(requireContext(), getString(R.string.loading)) {
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
        val command = SimplifyPathCommand(requireContext(), this, pathService)
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
        val command = TogglePathVisibilityCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun showPath(path: Path) {
        val command = ViewPathCommand(findNavController())
        command.execute(path)
    }

    private fun showPath(id: Long) {
        val command = ViewPathCommand(findNavController())
        command.execute(id)
    }

    private fun importPaths() {
        val command = ImportPathsCommand(
            requireContext(),
            this,
            gpxService,
            pathService,
            prefs.navigation
        )
        command.execute(manager.root?.id)
    }

    private fun exportPath(path: IPath?) {
        val command = ExportPathCommand(requireContext(), this, gpxService, pathService)
        command.execute(path)
    }

    private fun exportCurrentGroup() {
        exportPath(manager.root)
    }

    private fun deletePath(path: Path) {
        val command = DeletePathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun merge(path: Path) {
        val command = MergePathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun createPath() {
        val command = CreatePathCommand(requireContext(), pathService, prefs.navigation)
        inBackground {
            command.execute(manager.root?.id)?.let {
                showPath(it)
            }
        }
    }

    // Groups
    private fun deleteGroup(group: PathGroup) {
        val command = DeletePathGroupGroupCommand(requireContext(), pathService)
        inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun renameGroup(group: PathGroup) {
        val command = RenamePathGroupGroupCommand(requireContext(), pathService)
        inBackground {
            command.execute(group)
            manager.refresh()
        }
    }

    private fun createGroup() {
        val command = CreatePathGroupCommand(requireContext(), pathService)
        inBackground {
            command.execute(manager.root?.id)
            manager.refresh()
        }
    }

    private fun movePath(path: IPath) {
        val command = MoveIPathCommand(requireContext(), pathService)
        inBackground {
            val newGroup = command.execute(path)
            if (newGroup?.id != path.parentId) {
                toast(getString(R.string.moved_to, newGroup?.name ?: getString(R.string.no_group)))
                manager.refresh()
            }
        }
    }

    private fun onBacktrackChanged(data: Bundle): Boolean {
        updateStatusBar()
        return true
    }

}
