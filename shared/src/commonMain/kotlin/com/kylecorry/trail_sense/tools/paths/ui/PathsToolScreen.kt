package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.io.SharedGpxIOService
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
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
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathDialog
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathRequest
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ViewPathCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import kotlinx.coroutines.launch

@Composable
fun PathsToolScreen(
    modifier: Modifier = Modifier
) {
    val host = AppHost.current()
    val strings = remember { getAppService<AppStrings>() }
    val prefs = remember { getAppService<UserPreferences>() }
    val backtrack = remember { getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK) }
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        PathsViewModel(
            pathService = getAppService<IPathService>(),
            preferences = prefs.paths,
            backtrack = backtrack,
            gps = getAppService<SensorService>().getGPS()
        )
    }
    val formatService = remember { FormatService.getInstance() }
    val pathService = viewModel.pathService
    val gpxService = remember { SharedGpxIOService() }
    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
    var simplifyRequest by remember { mutableStateOf<SimplifyPathRequest?>(null) }
    val simplifyCommand = remember(host, scope) {
        SimplifyPathCommand(host, scope, pathService) { simplifyRequest = it }
    }
    val backtrackFrequencyTitle = appString(Res.string.pref_backtrack_frequency_title)

    fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> strings.get(Res.string.most_recent)
            PathSortMethod.Longest -> strings.get(Res.string.longest)
            PathSortMethod.Shortest -> strings.get(Res.string.shortest)
            PathSortMethod.Closest -> strings.get(Res.string.closest)
            PathSortMethod.Name -> strings.get(Res.string.name)
        }
    }

    fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            host,
            strings.get(Res.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(viewModel.sort)
        ) { newSort ->
            if (newSort != null) {
                viewModel.changeSort(sortOptions[newSort])
            }
        }
    }

    fun exportPath(path: IPath?) {
        ExportPathCommand(host, scope, gpxService, pathService).execute(path)
    }

    fun setGroupVisibility(group: PathGroup, visible: Boolean) {
        scope.launch {
            Alerts.withLoading(host, strings.get(Res.string.loading)) {
                viewModel.setGroupVisibility(group, visible)
            }
        }
    }

    fun movePath(path: IPath) {
        val command = MoveIPathCommand(host, pathService)
        scope.launch {
            val newGroup = command.execute(path)
            if (newGroup?.id != path.parentId) {
                effects.showMessage(
                    strings.get(Res.string.moved_to, newGroup?.name ?: strings.get(Res.string.no_group))
                )
                viewModel.refresh()
            }
        }
    }

    fun handleGroupAction(group: PathGroup, action: PathGroupAction) {
        when (action) {
            PathGroupAction.Delete -> scope.launch {
                DeletePathGroupGroupCommand(pathService).execute(group)
                viewModel.refresh()
            }

            PathGroupAction.Rename -> scope.launch {
                RenamePathGroupGroupCommand(pathService).execute(group)
                viewModel.refresh()
            }

            PathGroupAction.Open -> viewModel.open(group.id)
            PathGroupAction.Move -> movePath(group)
            PathGroupAction.Export -> exportPath(group)
            PathGroupAction.ShowAll -> setGroupVisibility(group, true)
            PathGroupAction.HideAll -> setGroupVisibility(group, false)
        }
    }

    fun handleAction(path: Path, action: PathAction) {
        when (action) {
            PathAction.Export -> exportPath(path)
            PathAction.Delete -> DeletePathCommand(scope, pathService).execute(path)
            PathAction.Merge -> MergePathCommand(host, scope, pathService).execute(path)
            PathAction.Show -> ViewPathCommand(appNavigator).execute(path)
            PathAction.Rename -> RenamePathCommand(scope, pathService).execute(path)
            PathAction.Keep -> KeepPathCommand(scope, pathService).execute(path)
            PathAction.ToggleVisibility -> TogglePathVisibilityCommand(scope, pathService).execute(path)
            PathAction.Simplify -> simplifyCommand.execute(path)
            PathAction.Move -> movePath(path)
            else -> {}
        }
    }

    // Back goes up a group; at the top it leaves the page as usual.
    SystemBackHandler(enabled = viewModel.handlesBack) {
        if (!viewModel.up()) {
            appNavigator.navigateUp()
        }
    }

    // Backtrack's state is followed while the page is shown.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {
            viewModel.onPause()
        }
    }

    PathsScreen(
        title = viewModel.groupName ?: appString(Res.string.paths),
        sortMenuText = appString(Res.string.sort_by, getSortString(viewModel.sort)),
        searchText = viewModel.searchText,
        items = viewModel.items,
        isLoaded = viewModel.isLoaded,
        resetKey = viewModel.resetKey,
        backtrackState = viewModel.backtrackState,
        backtrackFrequency = viewModel.backtrackFrequency,
        formatService = formatService,
        baseDistanceUnits = prefs.baseDistanceUnits,
        onBack = { if (!viewModel.up()) appNavigator.navigateUp() },
        onSearchChange = viewModel::search,
        onMenuAction = { action ->
            when (action) {
                PathsToolbarAction.Sort -> changeSort()
                PathsToolbarAction.Export -> exportPath(viewModel.root)
            }
        },
        onPathAction = ::handleAction,
        onGroupAction = ::handleGroupAction,
        onImportPath = {
            ImportPathsCommand(host, scope, gpxService, pathService, prefs.paths).execute(viewModel.rootId)
        },
        onCreateGroup = {
            scope.launch {
                CreatePathGroupCommand(pathService).execute(viewModel.rootId)
                viewModel.refresh()
            }
        },
        onCreatePathOnMap = { appNavigator.navigate(Route.Map(startPathCreation = true, pathGroupId = viewModel.rootId)) },
        onCreatePath = {
            scope.launch {
                CreatePathCommand(host, pathService, prefs.paths).execute(viewModel.rootId)?.let {
                    ViewPathCommand(appNavigator).execute(it)
                }
            }
        },
        onBacktrackFrequencyClick = if (backtrack?.isSupported == true) {
            { showBacktrackFrequencyPicker = true }
        } else null,
        onBacktrackToggleClick = if (backtrack?.isSupported == true) {
            {
                ToggleBacktrackCommand(effects, scope) {
                    effects.showMessage(strings.get(Res.string.backtrack_no_permission))
                }.execute()
            }
        } else null,
        modifier = modifier.fillMaxSize()
    )

    if (showBacktrackFrequencyPicker) {
        DurationPickerDialog(
            title = backtrackFrequencyTitle,
            duration = prefs.paths.backtrackRecordFrequency,
            message = appString(Res.string.actual_frequency_disclaimer),
            hint = appString(Res.string.frequency),
            showSeconds = true,
            onDurationPicked = {
                if (it != null) {
                    ChangeBacktrackFrequencyCommand(host, scope) {
                        viewModel.updateBacktrackStatus()
                    }.execute(it)
                }
                showBacktrackFrequencyPicker = false
            },
            onDismiss = {
                showBacktrackFrequencyPicker = false
            }
        )
    }

    simplifyRequest?.let { request ->
        SimplifyPathDialog(
            request = request,
            onApply = { simplifyCommand.apply(request.path, it) },
            onDismiss = { simplifyRequest = null }
        )
    }
}
