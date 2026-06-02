package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.PlayBarView
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.IPath
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

class PathsFragment : AndromedaFragment() {

    private var pathViews: PathsViews? = null
    private val binding: PathsViews
        get() = pathViews ?: error("Path views are not ready")

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createPathsViews(requireContext())
        pathViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PathsHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

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
            ) { updateStatusBar() }.execute()
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

private data class PathsViews(
    val root: View,
    val pathsTitle: PathsTitle,
    val searchbox: SearchView,
    val pathsList: AndromedaListView,
    val waypointsEmptyText: TextView,
    val overlayMask: ImageView,
    val addMenu: FloatingActionButtonMenu,
    val addBtn: FloatingActionButton,
    val backtrackPlayBar: PlayBarView
)

private data class PathsTitle(
    val root: View,
    val title: TextView,
    val rightButton: ImageButton
)

@Composable
private fun PathsHost(
    views: PathsViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createPathsViews(context: Context): PathsViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = createPathsTitle(context)
    root.addView(
        title.root,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val searchbox = SearchView(context, null).apply {
        id = R.id.searchbox
    }
    root.addView(
        searchbox,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.paths_title
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val playBar = PlayBarView(context, null).apply {
        id = R.id.backtrack_play_bar
        tag = context.getString(R.string.tag_play_bar)
        setImageResource(R.drawable.ic_tool_backtrack)
        this.title = context.getString(R.string.backtrack)
    }
    root.addView(
        playBar,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val list = AndromedaListView(context, null).apply {
        id = R.id.paths_list
    }
    root.addView(
        list,
        ConstraintLayout.LayoutParams(
            0,
            0
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.searchbox
            bottomToTop = R.id.backtrack_play_bar
            topMargin = 16.dp(context)
        }
    )

    val emptyText = TextView(context).apply {
        id = R.id.waypoints_empty_text
        text = context.getString(R.string.no_paths)
        textSize = 18f
        visibility = View.GONE
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    root.addView(
        emptyText,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.paths_list
            bottomToBottom = R.id.paths_list
            topMargin = 16.dp(context)
        }
    )

    val overlay = ImageView(context).apply {
        id = R.id.overlay_mask
        alpha = 0.86f
        setBackgroundColor(resolveThemeColor(context, android.R.attr.colorBackground))
        isClickable = true
        visibility = View.GONE
    }
    root.addView(
        overlay,
        ConstraintLayout.LayoutParams(
            0,
            0
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val addButton = FloatingActionButton(context).apply {
        id = R.id.add_btn
        setImageResource(R.drawable.ic_add)
        isClickable = true
        isFocusable = true
    }
    root.addView(
        addButton,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.backtrack_play_bar
            marginEnd = 16.dp(context)
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val addMenu = FloatingActionButtonMenu(context, null).apply {
        id = R.id.add_menu
        visibility = View.GONE
        setMenu(R.menu.add_path_menu)
    }
    root.addView(
        addMenu,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = R.id.add_btn
            bottomToTop = R.id.add_btn
            bottomMargin = 16.dp(context)
        }
    )

    return PathsViews(
        root = root,
        pathsTitle = title,
        searchbox = searchbox,
        pathsList = list,
        waypointsEmptyText = emptyText,
        overlayMask = overlay,
        addMenu = addMenu,
        addBtn = addButton,
        backtrackPlayBar = playBar
    )
}

private fun createPathsTitle(context: Context): PathsTitle {
    val root = LinearLayout(context).apply {
        id = R.id.paths_title
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(16.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
    }
    val title = TextView(context).apply {
        text = context.getString(R.string.paths)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    val menu = ImageButton(context).apply {
        id = R.id.paths_menu_btn
        contentDescription = context.getString(R.string.sort)
        setImageResource(R.drawable.ic_menu_dots)
        backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    root.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(menu, LinearLayout.LayoutParams(48.dp(context), 48.dp(context)))

    return PathsTitle(root, title, menu)
}

private fun resolveThemeColor(context: Context, attr: Int): Int {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return if (value.resourceId != 0) {
        ContextCompat.getColor(context, value.resourceId)
    } else {
        value.data
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun PathsHostPreview() {
    TrailSenseTheme {
        PathsHost(
            views = createPathsViews(LocalContext.current)
        )
    }
}
