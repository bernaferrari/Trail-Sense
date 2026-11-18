package com.kylecorry.trail_sense.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.navigation
import com.kylecorry.trail_sense.shared.generated.resources.settings
import com.kylecorry.trail_sense.shared.generated.resources.tools
import com.kylecorry.trail_sense.shared.generated.resources.weather
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SharedNavigationDisplay
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.shared.views.LocalErrorBanner
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.ActionSheetModal
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppHostDialogs
import com.kylecorry.trail_sense.shared.views.compose.AppHostOverlays
import com.kylecorry.trail_sense.shared.views.compose.AppHostSnackbar
import com.kylecorry.trail_sense.shared.views.compose.LocalAppBarInsets
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.SharedComposeDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.SharedDestinationContent
import org.jetbrains.compose.resources.StringResource

internal data class SharedAppTab(val route: Route, val label: StringResource)

private val appTabs = listOf(
    SharedAppTab(Route.Navigation(), Res.string.navigation),
    SharedAppTab(Route.Tools, Res.string.tools),
    SharedAppTab(Route.Weather, Res.string.weather),
    SharedAppTab(Route.Settings, Res.string.settings)
)

@Composable
internal fun SharedApp(
    strings: AppStrings,
    tabs: List<SharedAppTab> = appTabs,
    bindNavigation: @Composable (TrailSenseNavigator) -> Unit = {}
) {
    val navigator = remember { TrailSenseNavigator().apply { setStartRoute(Route.Tools) } }
    val host = remember { AppHost().also(AppHost::attach) }
    val errorBanner = remember { ErrorBannerState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(host, viewModelOwner, navigator) {
        onDispose {
            AppHost.detach(host)
            viewModelOwner.viewModelStore.clear()
        }
    }
    bindNavigation(navigator)

    val selectedIndex = tabs.indexOfFirst { it.route == navigator.currentRoute }
    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalNavigator provides navigator,
        LocalViewModelStoreOwner provides viewModelOwner,
        LocalErrorBanner provides errorBanner,
        LocalAppBarInsets provides WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    ) {
        TrailSenseTheme {
            SharedAdaptiveNavigation(strings, tabs, selectedIndex, { index ->
                tabs.getOrNull(index)?.route?.let { target ->
                    if (navigator.currentRoute != target) navigator.switchTab(target)
                }
            }) {
                Box(Modifier.fillMaxSize()) {
                    SharedNavigationDisplay(navigator, Modifier.fillMaxSize()) { route ->
                        val destinationHost = remember(route, navigator, lifecycleOwner, host, scope) {
                            SharedComposeDestinationHost(navigator, lifecycleOwner, route, host, scope)
                        }
                        CompositionLocalProvider(LocalScreenEffects provides destinationHost) {
                            destinationHost.SharedDestinationContent(Modifier.fillMaxSize())
                            ActionSheetModal(destinationHost)
                        }
                    }
                    AppHostOverlays(host)
                    AppHostSnackbar(host)
                    AppHostDialogs(host)
                }
            }
        }
    }
}

@Composable
private fun SharedAdaptiveNavigation(
    strings: AppStrings,
    tabs: List<SharedAppTab>,
    selected: Int,
    select: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val state = remember(strings, tabs, selected) {
        MainBottomNavigationState().apply {
            showAllLabels = true
            selectedRoute = tabs.getOrNull(selected)?.route
            items.addAll(tabs.map { tab ->
                val (icon, filled) = when (tab.route) {
                    is Route.Navigation -> TrailSenseIcons.Outlined.Explore to TrailSenseIcons.Filled.Explore
                    Route.Tools -> TrailSenseIcons.Outlined.Apps to TrailSenseIcons.Filled.Apps
                    Route.Weather -> TrailSenseIcons.Outlined.PartlyCloudyDay to TrailSenseIcons.Filled.PartlyCloudyDay
                    else -> TrailSenseIcons.Filled.Settings to TrailSenseIcons.Filled.Settings
                }
                MainBottomNavigationItem(tab.route, strings.get(tab.label), icon, filled, icon)
            })
        }
    }
    MainAdaptiveNavigation(
        state = state,
        onItemClick = { route -> select(tabs.indexOfFirst { it.route == route }) },
        onItemLongClick = {},
        showBar = selected >= 0,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        content = content
    )
}
