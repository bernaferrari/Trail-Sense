package com.kylecorry.trail_sense.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.device.DeviceSensors
import com.kylecorry.andromeda.device.LocationReading
import com.kylecorry.andromeda.preferences.Preferences
import com.mohamedrejeb.calf.sf.symbols.SFSymbol
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.AdaptiveScaffold
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.iosAppDatabase
import com.kylecorry.trail_sense.main.registerSharedRepositories
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.my_location
import com.kylecorry.trail_sense.shared.generated.resources.notes
import com.kylecorry.trail_sense.shared.generated.resources.navigation_nearby_category
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SharedNavigationDisplay
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppHostDialogs
import com.kylecorry.trail_sense.shared.views.compose.AppHostOverlays
import com.kylecorry.trail_sense.shared.views.compose.AppHostSnackbar
import com.kylecorry.trail_sense.tools.location.ui.LocationContent
import com.kylecorry.trail_sense.tools.beacons.BeaconBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.notes.infrastructure.INoteRepo
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorToolScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesToolScreen
import com.kylecorry.trail_sense.tools.qr.ui.QRShareScreen
import kotlinx.coroutines.CancellationException
import platform.UIKit.UIViewController

/** Entry point for the iOS host. [databasePath] is in the app's documents directory. */
@OptIn(ExperimentalCalfUiApi::class)
fun MainViewController(databasePath: String): UIViewController {
    val database = iosAppDatabase(databasePath)
    val strings = ComposeAppStrings()
    val preferences = Preferences()
    val userPreferences = UserPreferences(preferences, IosDeviceProfile())
    val formatter = FormatService(strings, IosPlatformFormats(), userPreferences)
    DependencyRegistry.addSingleton<AppStrings>(strings)
    DependencyRegistry.addSingleton(preferences)
    DependencyRegistry.addSingleton(userPreferences)
    DependencyRegistry.addSingleton(formatter)
    registerSharedRepositories(
        database = database,
        preferences = preferences,
        nearbyName = { strings.get(Res.string.navigation_nearby_category) },
        onPathsChanged = { ToolBroadcasts.broadcast(PathsBroadcasts.PATHS_CHANGED) },
        onBeaconsChanged = { ToolBroadcasts.broadcast(BeaconBroadcasts.BEACONS_CHANGED) }
    )
    val notes = getAppService<INoteRepo>()
    return ComposeUIViewController {
        val navigator = remember { TrailSenseNavigator().apply { setStartRoute(Route.Notes) } }
        val host = remember { AppHost().also(AppHost::attach) }
        val viewModelOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(host, viewModelOwner) {
            onDispose {
                AppHost.detach(host)
                viewModelOwner.viewModelStore.clear()
            }
        }
        var showLocation by remember { mutableStateOf(false) }
        var location by remember { mutableStateOf<LocationReading?>(null) }
        var locationUnavailable by remember { mutableStateOf(false) }
        val sensors = remember { DeviceSensors() }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(showLocation, lifecycleOwner) {
            if (showLocation) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    location = null
                    locationUnavailable = false
                    try {
                        sensors.location().collect { location = it.value }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        locationUnavailable = true
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalAppStrings provides strings,
            LocalNavigator provides navigator,
            LocalViewModelStoreOwner provides viewModelOwner
        ) {
            TrailSenseTheme {
                Box(Modifier.fillMaxSize()) {
                    AdaptiveScaffold(bottomBar = {
                        if (navigator.currentRoute == Route.Notes) {
                            AdaptiveNavigationBar(
                                iosItems = listOf(
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.notes),
                                        image = UIKitImage.SystemName(SFSymbol.listBulletRectangle),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.listBulletRectangleFill)
                                    ),
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.my_location),
                                        image = UIKitImage.SystemName(SFSymbol.location),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.locationFill)
                                    )
                                ),
                                iosSelectedIndex = if (showLocation) 1 else 0,
                                iosOnItemSelected = { index ->
                                    showLocation = index == 1
                                },
                                iosConfiguration = UIKitTabBarConfiguration(
                                    selectedItemColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }) { padding ->
                        SharedNavigationDisplay(navigator, Modifier.padding(padding)) { route ->
                            when (route) {
                                Route.Notes -> if (showLocation) {
                                    LocationContent(location, locationUnavailable)
                                } else {
                                    NotesToolScreen(notes, strings, formatter, showBack = false)
                                }
                                is Route.NoteEditor -> NoteEditorToolScreen(route, notes)
                                is Route.ViewQr -> QRShareScreen(route.title, route.text)
                                else -> Unit
                            }
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
