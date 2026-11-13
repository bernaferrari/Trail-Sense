@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.views.compose.from

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.item
import com.kylecorry.trail_sense.shared.dialogs.withLoading
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.PathsEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.ClosestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.LongestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.MostRecentPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.NamePathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.ShortestPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathGroupLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathsPreferences
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
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PathsToolScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        PathsViewModel(
            pathService = getAppService<IPathService>(),
            preferences = UserPreferences(appContext).paths,
            backtrack = getAppService<ToolCatalog>().getService(PathsToolRegistration.SERVICE_BACKTRACK),
            gps = getAppService<SensorService>().getGPS()
        )
    }
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val pathService = viewModel.pathService
    val gpxService = remember(context) {
        IOFactory().createGpxService(
            context,
            // File pickers need the activity's result launcher, an Activity-only API.
            requireNotNull(context.findMainActivity())
        )
    }
    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
    var simplifyRequest by remember { mutableStateOf<SimplifyPathRequest?>(null) }
    val simplifyCommand = remember(context, scope) {
        SimplifyPathCommand(context, scope, pathService) { simplifyRequest = it }
    }
    val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)
    val dispatcher = rememberSystemBack()

    fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> context.getString(R.string.most_recent)
            PathSortMethod.Longest -> context.getString(R.string.longest)
            PathSortMethod.Shortest -> context.getString(R.string.shortest)
            PathSortMethod.Closest -> context.getString(R.string.closest)
            PathSortMethod.Name -> context.getString(R.string.name)
        }
    }

    fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            context,
            context.getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(viewModel.sort)
        ) { newSort ->
            if (newSort != null) {
                viewModel.changeSort(sortOptions[newSort])
            }
        }
    }

    fun exportPath(path: IPath?) {
        ExportPathCommand(AppHost.from(context), scope, gpxService, pathService).execute(path)
    }

    fun setGroupVisibility(group: PathGroup, visible: Boolean) {
        scope.launch {
            Alerts.withLoading(context, context.getString(R.string.loading)) {
                viewModel.setGroupVisibility(group, visible)
            }
        }
    }

    fun movePath(path: IPath) {
        val command = MoveIPathCommand(AppHost.from(context), pathService)
        scope.launch {
            val newGroup = command.execute(path)
            if (newGroup?.id != path.parentId) {
                effects.showMessage(
                    context.getString(R.string.moved_to, newGroup?.name ?: context.getString(R.string.no_group))
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
            PathAction.Merge -> MergePathCommand(AppHost.from(context), scope, pathService).execute(path)
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
        title = viewModel.groupName ?: stringResource(R.string.paths),
        sortMenuText = stringResource(R.string.sort_by, getSortString(viewModel.sort)),
        searchText = viewModel.searchText,
        items = viewModel.items,
        isLoaded = viewModel.isLoaded,
        resetKey = viewModel.resetKey,
        backtrackState = viewModel.backtrackState,
        backtrackFrequency = viewModel.backtrackFrequency,
        formatService = formatService,
        baseDistanceUnits = prefs.baseDistanceUnits,
        onBack = { dispatcher?.invoke() },
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
            ImportPathsCommand(AppHost.current(), scope, gpxService, pathService, prefs.paths).execute(viewModel.rootId)
        },
        onCreateGroup = {
            scope.launch {
                CreatePathGroupCommand(pathService).execute(viewModel.rootId)
                viewModel.refresh()
            }
        },
        onCreatePathOnMap = { appNavigator.navigate(Route.Map(startPathCreation = true)) },
        onCreatePath = {
            scope.launch {
                CreatePathCommand(AppHost.from(context), pathService, prefs.paths).execute(viewModel.rootId)?.let {
                    ViewPathCommand(appNavigator).execute(it)
                }
            }
        },
        onBacktrackFrequencyClick = {
            showBacktrackFrequencyPicker = true
        },
        onBacktrackToggleClick = {
            ToggleBacktrackCommand(effects, scope).execute()
        },
        modifier = modifier.fillMaxSize()
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
                    ChangeBacktrackFrequencyCommand(AppHost.from(context), scope) {
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

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
